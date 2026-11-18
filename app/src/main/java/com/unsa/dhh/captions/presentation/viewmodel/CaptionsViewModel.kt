package com.unsa.dhh.captions.presentation.viewmodel

import android.app.Application
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unsa.dhh.captions.data.decision.DecisionEvaluator
import com.unsa.dhh.captions.data.decision.WordCorrectionResult
import com.unsa.dhh.captions.data.local.AppRoomDatabase
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.SessionLogEntity
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import com.unsa.dhh.captions.data.telemetry.EvaluationStorage
import com.unsa.dhh.captions.data.telemetry.SessionAudioRecorder
import com.unsa.dhh.captions.data.telemetry.SessionLogManager
import com.unsa.dhh.captions.data.telemetry.SessionStats
import com.unsa.dhh.captions.data.telemetry.UtteranceLogger
import com.unsa.dhh.captions.domain.asr.AndroidSpeechEngine
import com.unsa.dhh.captions.domain.asr.AsrEngineType
import com.unsa.dhh.captions.domain.asr.RawTranscriptionResult
import com.unsa.dhh.captions.domain.asr.SpeechEngine
import com.unsa.dhh.captions.domain.asr.SpeechState
import com.unsa.dhh.captions.domain.asr.VoskSpeechEngine
import com.unsa.dhh.captions.presentation.NotesExporter
import com.unsa.dhh.captions.service.AudioInputSource
import com.unsa.dhh.captions.service.ExternalAudioDeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID

/**
 * ViewModel principal del prototipo de subtitulado accesible.
 * Coordina la cadena completa: ASR -> PLN -> Inferencia Mamdani -> Room DB -> Teleprompter.
 */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest: los colectores siguen al motor ASR activo
class CaptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppRoomDatabase.getDatabase(application)
    private val dictionaryDao = database.dictionaryDao()
    private val sessionLogDao = database.sessionLogDao()

    private val decisionEvaluator = DecisionEvaluator()
    private val sessionLogManager = SessionLogManager(sessionLogDao, viewModelScope)
    private val notesExporter = NotesExporter(application)
    private val audioDeviceManager = ExternalAudioDeviceManager(application)

    /** Carpeta privada usada hasta el 18/11/2026 (se copia a la pública al arrancar). */
    private val legacyEvaluationDir =
        File(application.getExternalFilesDir(null) ?: application.filesDir, EvaluationStorage.PRIVATE_FOLDER)

    /** Android 11+: `Documentos/PrototipoFuzzy/`, una carpeta por sesión (CSV + WAV). Ver [EvaluationStorage]. */
    private val evaluationDir = EvaluationStorage.chooseDir(
        publicDocuments = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        } else null,
        privateBase = application.getExternalFilesDir(null) ?: application.filesDir
    ).also { dir ->
        // CSV y textos (pocos KB) antes de que el registro escriba en la carpeta nueva: así los archivos de nombre
        // fijo (NASA-TLX, SUS, indice_audio.csv) conservan sus filas anteriores. Los WAV, en segundo plano.
        EvaluationStorage.migrateLegacy(legacyEvaluationDir, dir, wavs = false)
        Log.i("CaptionsViewModel", "Datos de evaluación en ${dir.absolutePath}")
    }
    private val utteranceLogger = UtteranceLogger(evaluationDir)

    /** Grabación del docente: WAV con el mismo audio que reconoce Vosk, en la carpeta de cada sesión. */
    private val audioRecorder = SessionAudioRecorder(evaluationDir)
    val audioRecordingStatus: StateFlow<SessionAudioRecorder.Status> = audioRecorder.status

    // ---- Motores ASR (se crean al elegirlos por primera vez; Vosk carga su modelo al crearse) ----
    private val voskEngine = lazy {
        VoskSpeechEngine(application, { audioDeviceManager.preferredInputDevice() }, audioRecorder)
    }
    private val androidEngine = lazy { AndroidSpeechEngine(application) }

    private val _asrEngineType = MutableStateFlow(AsrEngineType.VOSK)
    val asrEngineType: StateFlow<AsrEngineType> = _asrEngineType.asStateFlow()

    private fun engineFor(type: AsrEngineType): SpeechEngine = when (type) {
        AsrEngineType.VOSK -> voskEngine.value
        AsrEngineType.ANDROID -> androidEngine.value
    }

    private val speechEngine: SpeechEngine get() = engineFor(_asrEngineType.value)

    /**
     * Términos del diccionario que Vosk NO puede escribir (no están en su vocabulario); los parte en pedazos
     * ("sito plasma"). La capa difusa usa C neutra al unir esos pedazos. Se regenera con
     * evaluation/vosk_fase0/generar_oov.py si cambia el modelo o el diccionario.
     */
    private val voskOutOfVocabulary: Set<String> by lazy {
        try {
            getApplication<Application>().assets.open("dictionaries/biologia_vosk_oov.json").use { input ->
                InputStreamReader(input).use { reader ->
                    Gson().fromJson<List<String>>(reader, object : TypeToken<List<String>>() {}.type)
                        .map { it.lowercase(Locale.ROOT) }.toSet()
                }
            }
        } catch (e: Exception) {
            Log.w("CaptionsViewModel", "Sin lista de términos fuera del vocabulario de Vosk: ${e.message}")
            emptySet()
        }
    }
    val audioInputSource: StateFlow<AudioInputSource> = audioDeviceManager.currentSource
    val isExternalMicConnected: StateFlow<Boolean> = audioDeviceManager.isExternalMicConnected

    // Estado de la sesión actual
    private var currentSessionId = UUID.randomUUID().toString()
    private var utteranceIndex = 0

    // ---- Panel del investigador (oculto: pulsación larga sobre el título) ----
    /** true = T2 (se muestra el texto corregido); false = T0 (se muestra el texto crudo del ASR). */
    private val _correctionEnabled = MutableStateFlow(true)
    val correctionEnabled: StateFlow<Boolean> = _correctionEnabled.asStateFlow()

    private val _participantCode = MutableStateFlow("")
    val participantCode: StateFlow<String> = _participantCode.asStateFlow()

    private val _sessionLabel = MutableStateFlow("")
    val sessionLabel: StateFlow<String> = _sessionLabel.asStateFlow()

    private val _evaluationFilePath = MutableStateFlow<String?>(null)
    val evaluationFilePath: StateFlow<String?> = _evaluationFilePath.asStateFlow()

    /** Escala del tamaño de letra del teleprompter (1.0 = tamaño base del dispositivo). */
    private val _textScale = MutableStateFlow(1.0f)
    val textScale: StateFlow<Float> = _textScale.asStateFlow()

    fun increaseTextSize() { _textScale.value = roundScale(_textScale.value + TEXT_SCALE_STEP).coerceAtMost(TEXT_SCALE_MAX) }
    fun decreaseTextSize() { _textScale.value = roundScale(_textScale.value - TEXT_SCALE_STEP).coerceAtLeast(TEXT_SCALE_MIN) }

    /** Redondea a un decimal para evitar acumular error de coma flotante (1.2000001). */
    private fun roundScale(v: Float): Float = kotlin.math.round(v * 10f) / 10f

    /** "telefono" o "tableta", según el ancho mínimo de pantalla (no cambia al girar el equipo). */
    private fun deviceType(): String =
        com.unsa.dhh.captions.presentation.DeviceClass.label(getApplication<Application>().resources.configuration)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    /**
     * Modo alumno: pantalla horizontal fija, fijada (screen pinning) y siempre encendida; el alumno solo puede
     * desplazar el texto. Lo activa el investigador desde el panel y se quita con [UNLOCK_PIN].
     * MainActivity aplica las restricciones del sistema (orientación, fijación, barras, pantalla encendida).
     */
    private val _studentLock = MutableStateFlow(false)
    val studentLock: StateFlow<Boolean> = _studentLock.asStateFlow()

    /** Bloquea para el alumno. Si el micrófono estaba detenido, lo inicia: el modo alumno es para transcribir. */
    fun lockForStudent() {
        if (!_isListening.value) toggleListening()
        _studentLock.value = true
    }

    /** true si el PIN es correcto y se desbloqueó. */
    fun unlockStudent(pin: String): Boolean {
        if (pin.trim() != UNLOCK_PIN) return false
        _studentLock.value = false
        return true
    }

    private val _teleprompterLines = MutableStateFlow<List<String>>(emptyList())
    val teleprompterLines: StateFlow<List<String>> = _teleprompterLines.asStateFlow()

    private val _rawTranscriptText = MutableStateFlow("")
    val rawTranscriptText: StateFlow<String> = _rawTranscriptText.asStateFlow()

    private val _correctedTranscriptText = MutableStateFlow("")
    val correctedTranscriptText: StateFlow<String> = _correctedTranscriptText.asStateFlow()

    private val _recentCorrections = MutableStateFlow<List<WordCorrectionResult>>(emptyList())
    val recentCorrections: StateFlow<List<WordCorrectionResult>> = _recentCorrections.asStateFlow()

    private val _sessionStats = MutableStateFlow(SessionStats(0, 0, 0L, 0.0, 0.0))
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()

    private val _lastExportMessage = MutableStateFlow<String?>(null)
    val lastExportMessage: StateFlow<String?> = _lastExportMessage.asStateFlow()

    /** Aviso informativo del modo de reconocimiento (p. ej. "usando reconocimiento en línea"). No se borra al empezar a escuchar. */
    val asrModeNotice: StateFlow<String?> = _asrEngineType
        .flatMapLatest { engineFor(it).modeNotice }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _asrStatusMessage = MutableStateFlow<String?>(null)
    val asrStatusMessage: StateFlow<String?> = _asrStatusMessage.asStateFlow()

    // Acumuladores de transcripción permanente para evitar duplicidad de texto
    private val processingLock = Any()
    private val committedLines = mutableListOf<String>()
    private var activePartialLine: String = ""

    // Diccionario en memoria rápida de la asignatura activa
    private var activeDictionary: List<AcademicTerm> = emptyList()
    // Clusters temáticos de co-ocurrencia semántica para la asignatura activa
    private var activeClusters: List<TopicCluster> = emptyList()

    init {
        loadDictionaryForSubject(SUBJECT_CODE)
        observeSpeechEngine()
        audioDeviceManager.startMonitoring()
        startEvaluationSession()
        // Grabaciones anteriores (cientos de MB): se copian sin frenar el arranque; nunca chocan con las nuevas
        viewModelScope.launch(Dispatchers.IO) {
            val n = EvaluationStorage.migrateLegacy(legacyEvaluationDir, evaluationDir, wavs = true)
            if (n > 0) Log.i("CaptionsViewModel", "Copiados $n audios anteriores a ${evaluationDir.absolutePath}")
        }
    }

    /**
     * Interruptor experimental T0/T2. Cambiar de condición limpia la pantalla y abre un archivo nuevo,
     * para que cada archivo de evaluación contenga una sola condición.
     */
    fun setCorrectionEnabled(enabled: Boolean) {
        if (enabled == _correctionEnabled.value) return
        _correctionEnabled.value = enabled
        clearTranscription()
    }

    /** Código anónimo del participante (p. ej. E01, D02) y etiqueta libre de la sesión (p. ej. "piloto", "clase1"). */
    fun setSessionIdentity(participant: String, label: String) {
        _participantCode.value = participant.trim()
        _sessionLabel.value = label.trim()
        clearTranscription()
    }

    private fun conditionCode(): String = if (_correctionEnabled.value) "T2_CORREGIDO" else "T0_SIN_CORRECCION"

    private fun startEvaluationSession() {
        utteranceIndex = 0
        val app = getApplication<Application>()
        val version = try {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
        } catch (e: Exception) { "?" }
        utteranceLogger.startSession(
            UtteranceLogger.SessionInfo(
                sessionId = currentSessionId,
                participant = _participantCode.value,
                label = _sessionLabel.value,
                condition = conditionCode(),
                subject = SUBJECT_CODE,
                audioSource = audioInputSource.value.displayName,
                deviceType = deviceType(),
                device = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                appVersion = version
            )
        )
        _evaluationFilePath.value = utteranceLogger.currentFilePath
        val sessionDir = utteranceLogger.currentSessionDir
        utteranceLogger.currentBaseName?.let { base ->
            if (sessionDir != null) audioRecorder.startSession(base, currentSessionId, sessionDir)
        }
    }

    /**
     * Grabar (o no) el audio del docente. Solo con el motor Vosk: el reconocedor de Android usa el micrófono en
     * exclusiva y no entrega el audio a la app.
     */
    fun setAudioRecordingEnabled(enabled: Boolean) {
        audioRecorder.setEnabled(enabled)
    }

    /** Guarda las respuestas del SUS y de los ítems complementarios (sin puntaje) con el código del participante. */
    fun saveSusResponses(responses: List<Int>, complementary: List<Int>) {
        utteranceLogger.logSus(_participantCode.value, _sessionLabel.value, deviceType(), responses, complementary)
    }

    /**
     * Guarda el NASA-TLX de la mitad de la clase que acaba de terminar, con la condición activa (T0 o T2).
     * El estudiante no ve la condición; por eso se responde ANTES de que el investigador la cambie.
     */
    fun saveTlxResponses(values: List<Int>) {
        val condition = if (_correctionEnabled.value) "T2" else "T0"
        utteranceLogger.logTlx(_participantCode.value, _sessionLabel.value, deviceType(), condition, values)
    }

    private fun loadDictionaryForSubject(subject: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Fuente de verdad: el diccionario del APK, en el orden del JSON (el orden decide los empates de D y
            // es el mismo que usan las pruebas y el espejo Python). Antes se leía Room, que solo se llenaba al
            // crearse la base: tras actualizar la app seguía con el diccionario de la primera instalación.
            var terms = loadDictionaryFromAssets(subject)
            if (terms.isNotEmpty()) {
                try {
                    dictionaryDao.replaceSubject(subject, terms)
                } catch (e: Exception) {
                    Log.w("CaptionsViewModel", "No se pudo actualizar el diccionario en Room DB: ${e.message}")
                }
            } else {
                terms = dictionaryDao.getTermsBySubject(subject)
            }
            activeDictionary = terms
            activeClusters = loadClustersFromAssets(subject)
            Log.i("CaptionsViewModel", "Diccionario activo para $subject: ${activeDictionary.size} términos, ${activeClusters.size} clusters temáticos.")
        }
    }

    private fun loadDictionaryFromAssets(subject: String): List<AcademicTerm> {
        return try {
            val fileName = "dictionaries/${subject.lowercase(Locale.ROOT)}.json"
            getApplication<Application>().assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val listType = object : TypeToken<List<AcademicTerm>>() {}.type
                    Gson().fromJson(reader, listType)
                }
            }
        } catch (e: Exception) {
            Log.e("CaptionsViewModel", "Error cargando fallback asset $subject: ${e.message}")
            emptyList()
        }
    }

    private fun loadClustersFromAssets(subject: String): List<TopicCluster> {
        return try {
            val fileName = "clusters/${subject.lowercase(Locale.ROOT)}_clusters.json"
            getApplication<Application>().assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val listType = object : TypeToken<List<TopicCluster>>() {}.type
                    Gson().fromJson(reader, listType) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            Log.d("CaptionsViewModel", "Sin archivo de clusters para $subject (${e.message})")
            emptyList()
        }
    }

    private fun observeSpeechEngine() {
        // Procesamiento SECUENCIAL: un único colector consume los resultados del ASR en orden de llegada.
        // Antes cada resultado lanzaba su propia corrutina en Dispatchers.Default y varias mutaban
        // `committedLines`/`activePartialLine` a la vez, con riesgo de que un parcial lento pisara a una final.
        viewModelScope.launch(Dispatchers.Default) {
            _asrEngineType.flatMapLatest { engineFor(it).transcriptionStream }.collect { rawResult ->
                // Un parcial que ya fue superado por otro más nuevo (o por la oración final) no se procesa:
                // corregirlo solo retrasaría la oración final que espera detrás. Las finales se procesan siempre.
                if (!rawResult.isFinal && rawResult.sequence < speechEngine.latestResultSequence()) return@collect
                processResultSequentially(rawResult)
            }
        }

        // Al conectar/desconectar el micrófono el reconocedor abierto queda atado al dispositivo anterior
        // (síntoma: "Error del cliente ASR"); se recrea sin intervención del usuario.
        viewModelScope.launch {
            var previous: AudioInputSource? = null
            audioInputSource.collect { source ->
                if (previous != null && previous != source) speechEngine.restartForAudioSourceChange()
                previous = source
            }
        }

        viewModelScope.launch {
            _asrEngineType.flatMapLatest { engineFor(it).engineState }.collect { state ->
                when (state) {
                    is SpeechState.Listening -> {
                        _isListening.value = true
                        _asrStatusMessage.value = null
                    }
                    is SpeechState.Error -> {
                        if (state.errorCode != 6 && state.errorCode != 7) {
                            _asrStatusMessage.value = state.message
                        }
                    }
                    is SpeechState.Idle -> {
                        _isListening.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Obtiene el historial completo de correcciones difusas de la sesión.
     */
    fun getCorrectionsHistory(): List<SessionLogEntity> {
        return sessionLogManager.getCorrectionsHistory()
    }

    /**
     * Procesa la ráfaga ASR diferenciando hipótesis parciales de oraciones finales.
     * Evita la duplicación de texto y mantiene todo el historial scrollable.
     */
    fun processIncomingRawText(rawResult: RawTranscriptionResult) {
        if (rawResult.words.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) { processResultSequentially(rawResult) }
    }

    /**
     * Núcleo del procesamiento. Se serializa con [processingLock] para que el flujo del ASR y las
     * inyecciones manuales (modo de prueba de la UI) nunca modifiquen el estado de la transcripción a la vez.
     */
    private fun processResultSequentially(rawResult: RawTranscriptionResult) = synchronized(processingLock) {
        val words = rawResult.words
        val confidences = rawResult.wordConfidences
        if (words.isEmpty()) return@synchronized

        val startTime = System.currentTimeMillis()
        val startNanos = System.nanoTime()

        // La corrección se calcula SIEMPRE (también en T0), para registrar crudo y corregido del mismo enunciado.
        val correctionResults = decisionEvaluator.processSentence(
            rawWords = words,
            confidences = confidences,
            dictionary = activeDictionary,
            clusters = activeClusters,
            updateLessonMemory = rawResult.isFinal, // solo oraciones finales alimentan la memoria de la clase
            asrOutOfVocabulary = if (_asrEngineType.value == AsrEngineType.VOSK) voskOutOfVocabulary else emptySet()
        )

        val correctionMs = (System.nanoTime() - startNanos) / 1_000_000.0
        val correctionDoneAtMs = SystemClock.elapsedRealtime()
        val latencyMs = System.currentTimeMillis() - startTime
        val correctedSentence = correctionResults.joinToString(" ") { it.finalWord }
        val showCorrected = _correctionEnabled.value
        val displayedSentence = if (showCorrected) correctedSentence else words.joinToString(" ")

        if (rawResult.isFinal) {
            // Telemetría: solo oraciones FINALES. Registrar también los parciales duplicaba cada
            // corrección en el historial y en las estadísticas (la final repite las mismas palabras).
            // En T0 no se alimenta el historial visible: el estudiante no debe ver correcciones que no se aplicaron.
            if (showCorrected) {
                sessionLogManager.recordBatch(
                    sessionId = currentSessionId,
                    subject = SUBJECT_CODE,
                    results = correctionResults,
                    latencyMs = latencyMs
                )
            }

            if (displayedSentence.isNotBlank()) {
                committedLines.add(displayedSentence)
            }
            activePartialLine = ""
            _recentCorrections.value = if (showCorrected) correctionResults.filter { it.wasSubstituted } else emptyList()
            updateSessionStats()
        } else {
            activePartialLine = displayedSentence
        }

        val currentAllLines = if (activePartialLine.isBlank()) {
            committedLines.toList()
        } else {
            committedLines.toList() + activePartialLine
        }

        _teleprompterLines.value = currentAllLines
        _correctedTranscriptText.value = currentAllLines.joinToString("\n")
        _rawTranscriptText.value = currentAllLines.joinToString("\n")

        if (rawResult.isFinal) {
            // t_publicado: el texto ya está en el estado que observa la UI (Compose lo dibuja en el siguiente cuadro, ~16 ms).
            val publishedAtMs = SystemClock.elapsedRealtime()
            utteranceLogger.log(
                UtteranceLogger.Utterance(
                    index = ++utteranceIndex,
                    wallClockMs = rawResult.timestamp,
                    asrProfile = rawResult.asrProfile,
                    rawConfidences = rawResult.rawConfidences,
                    // C media del enunciado (con Vosk hay una C por palabra; el detalle va en palabras_evaluadas)
                    confidenceUsed = if (confidences.isEmpty()) Float.NaN else confidences.average().toFloat(),
                    rawText = words.joinToString(" "),
                    correctedText = correctedSentence,
                    displayedText = displayedSentence,
                    beginOfSpeechAtMs = rawResult.beginOfSpeechAtMs,
                    firstPartialAtMs = rawResult.firstPartialAtMs,
                    endOfSpeechAtMs = rawResult.endOfSpeechAtMs,
                    resultsAtMs = rawResult.resultsAtMs,
                    correctionDoneAtMs = correctionDoneAtMs,
                    publishedAtMs = publishedAtMs,
                    correctionMs = correctionMs,
                    audioSource = audioInputSource.value.displayName,
                    textScale = _textScale.value,
                    words = correctionResults
                )
            )
        }
    }

    private fun updateSessionStats() {
        val keywords = activeDictionary.map { it.term }.toSet()
        _sessionStats.value = sessionLogManager.getSessionStats(keywords)
    }

    fun toggleListening() {
        if (_isListening.value) {
            speechEngine.stopListening()
            _isListening.value = false
            _asrStatusMessage.value = null
        } else {
            _asrStatusMessage.value = null
            speechEngine.startListening()
            _isListening.value = true
        }
    }

    /** Reintento manual tras un error: detiene (si hacía falta) y vuelve a iniciar la escucha. */
    fun retryListening() {
        _asrStatusMessage.value = null
        speechEngine.stopListening()
        speechEngine.startListening()
        _isListening.value = true
    }

    fun clearAsrStatusMessage() {
        _asrStatusMessage.value = null
    }

    fun exportNotesAsTxt() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = notesExporter.exportToTxt(SUBJECT_DISPLAY_NAME, _correctedTranscriptText.value)
            _lastExportMessage.value = if (file != null) "Apuntes TXT guardados: ${file.name}" else "Error al guardar TXT"
        }
    }

    fun exportNotesAsPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = notesExporter.exportToPdf(SUBJECT_DISPLAY_NAME, _correctedTranscriptText.value)
            _lastExportMessage.value = if (file != null) "Apuntes PDF guardados: ${file.name}" else "Error al guardar PDF"
        }
    }

    fun clearExportMessage() {
        _lastExportMessage.value = null
    }

    fun clearTranscription() = synchronized(processingLock) {
        committedLines.clear()
        activePartialLine = ""
        _rawTranscriptText.value = ""
        _correctedTranscriptText.value = ""
        _teleprompterLines.value = emptyList()
        currentSessionId = UUID.randomUUID().toString()
        sessionLogManager.resetSession()
        decisionEvaluator.resetLesson()
        _sessionStats.value = SessionStats(0, 0, 0L, 0.0, 0.0, 0.0, 0.0, 1.0)
        _recentCorrections.value = emptyList()
        _asrStatusMessage.value = null
        startEvaluationSession()
    }

    /**
     * Cambia el motor ASR (panel del investigador). Detiene el motor anterior, abre una sesión nueva (pantalla
     * limpia + archivo nuevo: un archivo = un motor, queda en perfil_asr) y retoma la escucha si estaba activa.
     */
    fun setAsrEngine(type: AsrEngineType) {
        if (type == _asrEngineType.value) return
        val wasListening = _isListening.value
        speechEngine.stopListening()
        _isListening.value = false
        // Crear el motor nuevo AQUÍ (hilo principal): SpeechRecognizer exige crearse en él, y si no lo
        // crearía el colector de transcripciones, que corre en Dispatchers.Default.
        engineFor(type)
        _asrEngineType.value = type
        clearTranscription()
        if (wasListening) {
            speechEngine.startListening()
            _isListening.value = true
        }
    }

    override fun onCleared() {
        if (voskEngine.isInitialized()) voskEngine.value.destroy()
        if (androidEngine.isInitialized()) androidEngine.value.destroy()
        audioRecorder.close() // cierra el WAV con su cabecera final
        audioDeviceManager.stopMonitoring()
        super.onCleared()
    }

    companion object {
        /**
         * Única área curricular de la investigación: Ciencia y Tecnología (CNEB), contenidos de biología.
         * El código interno del diccionario y de los grupos temáticos sigue siendo "BIOLOGIA"
         * (assets/dictionaries/biologia.json, assets/clusters/biologia_clusters.json).
         */
        const val SUBJECT_CODE = "BIOLOGIA"
        const val SUBJECT_DISPLAY_NAME = "Ciencia y Tecnología"

        /** PIN del investigador para salir del modo alumno (columna central del teclado). */
        const val UNLOCK_PIN = "2580"

        const val TEXT_SCALE_MIN = 0.8f
        const val TEXT_SCALE_MAX = 1.6f
        const val TEXT_SCALE_STEP = 0.1f
    }
}
