package com.unsa.dhh.captions.domain.asr

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Módulo 2: Motor ASR Local de Android.
 * Utiliza la API nativa SpeechRecognizer con soporte de captura continua en tiempo real.
 * Auto-reinicia la escucha en silencios naturales para subtitulado continuo.
 * Extrae las puntuaciones de confianza acústica C in [0.0, 1.0] por palabra/frase.
 */
class AndroidSpeechEngine(private val context: Context) : SpeechEngine, RecognitionListener {

    /**
     * Configuración de reconocimiento. Se prueban en orden hasta que una funcione en el equipo.
     */
    private data class RecognitionProfile(
        val label: String,
        /** null = no forzar idioma: el reconocedor usa el idioma del sistema. */
        val languageTag: String?,
        val preferOffline: Boolean,
        val onDevice: Boolean = false,
        val needsInternet: Boolean = false
    )

    /**
     * Sin conexión: español de EE. UU. (es-US), el único paquete de español que ofrece el teléfono de prueba (Tecno Spark 20).
     * En línea: es-419 (Latinoamérica), que reconoce mejor el acento peruano. El modo en línea es solo el último recurso.
     */
    private val profiles: List<RecognitionProfile> = buildList {
        if (Build.VERSION.SDK_INT >= 33 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            add(RecognitionProfile("reconocimiento local del equipo (es-US)", OFFLINE_LANGUAGE_TAG, true, onDevice = true))
        }
        add(RecognitionProfile("sin conexión preferido (es-US)", OFFLINE_LANGUAGE_TAG, true))
        add(RecognitionProfile("reconocimiento en línea (es-419)", ONLINE_LANGUAGE_TAG, false, needsInternet = true))
    }

    private val policy = RecognitionFallbackPolicy(profiles.size)
    private fun currentProfile() = profiles[policy.profileIndex]

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isContinuousListeningEnabled = false
    private var gaveUp = false

    /** Reinicio pendiente. Solo puede haber uno: reinicios solapados provocan ERROR_CLIENT. */
    private var restartJob: Job? = null

    private val _engineState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val engineState: StateFlow<SpeechState> = _engineState.asStateFlow()

    private val _transcriptionStream = MutableSharedFlow<RawTranscriptionResult>(replay = 0)
    override val transcriptionStream: SharedFlow<RawTranscriptionResult> = _transcriptionStream.asSharedFlow()

    /** Contador de resultados emitidos; el consumidor lo usa para saltarse parciales ya superados. */
    private val resultSequence = java.util.concurrent.atomic.AtomicLong(0)

    /** Número del último resultado (parcial o final) producido por el reconocedor. */
    override fun latestResultSequence(): Long = resultSequence.get()

    /** Aviso informativo cuando se tuvo que cambiar de modo (p. ej. a reconocimiento en línea). null = modo preferido. */
    private val _modeNotice = MutableStateFlow<String?>(null)
    override val modeNotice: StateFlow<String?> = _modeNotice.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        val profile = currentProfile()
        val canUseOnDevice = profile.onDevice && Build.VERSION.SDK_INT >= 33

        if (!canUseOnDevice && !SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "SpeechRecognizer no está disponible en este dispositivo.")
            _engineState.value = SpeechState.Error("Reconocimiento de voz no disponible en el sistema (falta la app de Google o los servicios de voz)", -1)
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error al limpiar el reconocedor previo: ${e.message}")
        }
        speechRecognizer = null

        try {
            speechRecognizer = (if (canUseOnDevice) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }).apply { setRecognitionListener(this@AndroidSpeechEngine) }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo instanciar SpeechRecognizer: ${e.message}", e)
            _engineState.value = SpeechState.Error("Error al iniciar el motor ASR nativo", -1)
            return
        }

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            profile.languageTag?.let { tag ->
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
            }
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Módulo 2: operación on-device preferida (TC-NET-01). Si el equipo no tiene el paquete de español
            // sin conexión, la política de recuperación pasa al siguiente perfil y se avisa al usuario.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, profile.preferOffline)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }
        Log.i(TAG, "Reconocedor inicializado: ${profile.label}")
    }

    /**
     * Inicia la escucha continua de la voz del docente.
     */
    override fun startListening() {
        if (gaveUp) {
            // El usuario reintenta tras un fallo total: volver a empezar por el mejor perfil
            policy.reset()
            gaveUp = false
            diagnosticsRequested = false
            _modeNotice.value = null
            initializeRecognizer()
        }
        isContinuousListeningEnabled = true
        restartJob?.cancel()
        try {
            if (speechRecognizer == null) {
                initializeRecognizer()
            }
            speechRecognizer?.startListening(recognizerIntent)
            _engineState.value = SpeechState.Listening
            Log.d(TAG, "SpeechRecognizer iniciado en modo continuo.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar escucha: ${e.message}", e)
            _engineState.value = SpeechState.Error(e.message ?: "Error desconocido al iniciar ASR", -1)
        }
    }

    /**
     * Detiene la escucha continua.
     */
    override fun stopListening() {
        isContinuousListeningEnabled = false
        restartJob?.cancel()
        try {
            speechRecognizer?.stopListening()
            _engineState.value = SpeechState.Idle
            Log.d(TAG, "SpeechRecognizer detenido por el usuario.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener escucha: ${e.message}", e)
        }
    }

    /**
     * Reinicia el reconocedor sin que el usuario lo note. Se usa cuando cambia el micrófono (p. ej. se conecta el
     * receptor USB): el reconocedor abierto sigue atado al dispositivo de audio anterior y falla con ERROR_CLIENT.
     */
    override fun restartForAudioSourceChange() {
        if (!isContinuousListeningEnabled) return
        Log.i(TAG, "Cambió la fuente de audio: recreando el reconocedor")
        scheduleRestart(delayMs = 400, recreate = true)
    }

    private var diagnosticsRequested = false

    /**
     * Pregunta al reconocedor del equipo qué idiomas ofrece y, en Android 13+, qué paquetes sin conexión tiene
     * instalados; si el español de Perú está disponible pero no descargado, solicita su descarga.
     * El resultado se añade al aviso de modo para que el usuario vea la etiqueta REAL que usa su teléfono.
     */
    private fun runLanguageDiagnostics() {
        if (diagnosticsRequested) return
        diagnosticsRequested = true

        // 1) Idiomas que declara el servicio de voz (todas las versiones)
        try {
            val details = RecognizerIntent.getVoiceDetailsIntent(context)
            if (details != null) {
                context.sendOrderedBroadcast(details, null, object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, i: Intent?) {
                        val extras = getResultExtras(false)
                        val langs = extras?.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                        val spanish = langs?.filter { it.startsWith("es", ignoreCase = true) }
                        Log.i(TAG, "Idiomas del servicio de voz: ${langs?.size ?: 0}; español: $spanish")
                        appendDiagnostic(
                            if (langs == null) "El servicio de voz no informó sus idiomas."
                            else "Idiomas en español que reporta tu servicio de voz: " +
                                (spanish?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "ninguno") + "."
                        )
                    }
                }, null, Activity.RESULT_OK, null, null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo consultar los idiomas del servicio de voz: ${e.message}")
        }

        // 2) Paquetes sin conexión (Android 13+): informa y pide la descarga si falta es-PE
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                val recognizer = speechRecognizer ?: return
                val intent = recognizerIntent ?: return
                recognizer.checkRecognitionSupport(
                    intent, context.mainExecutor,
                    object : android.speech.RecognitionSupportCallback {
                        override fun onSupportResult(support: android.speech.RecognitionSupport) {
                            val installed = support.installedOnDeviceLanguages
                            val available = support.supportedOnDeviceLanguages
                            Log.i(TAG, "Sin conexión: instalados=$installed disponibles=$available")
                            val tag = "es-PE"
                            val canDownload = available.any { it.equals(tag, true) } && installed.none { it.equals(tag, true) }
                            if (canDownload) {
                                try {
                                    recognizer.triggerModelDownload(intent)
                                    appendDiagnostic("Se solicitó la descarga del paquete sin conexión $tag; espera a que termine y reintenta.")
                                } catch (e: Exception) {
                                    appendDiagnostic("No se pudo iniciar la descarga de $tag: ${e.message}")
                                }
                            } else {
                                appendDiagnostic(
                                    "Paquetes sin conexión instalados: " + (installed.filter { it.startsWith("es", true) }
                                        .joinToString(", ").ifEmpty { "ninguno en español" }) + "."
                                )
                            }
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "checkRecognitionSupport falló: $error")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "checkRecognitionSupport no disponible: ${e.message}")
            }
        }
    }

    private fun appendDiagnostic(text: String) {
        // Solo bitácora: el aviso de diagnóstico no se muestra al usuario
        Log.i(TAG, text)
    }

    /** Programa UN reinicio (cancela cualquiera pendiente). */
    private fun scheduleRestart(delayMs: Long, recreate: Boolean) {
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs)
            if (!isContinuousListeningEnabled) return@launch
            try {
                if (recreate) {
                    initializeRecognizer()
                } else {
                    speechRecognizer?.cancel()
                }
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error al reiniciar ASR: ${e.message}")
            }
        }
    }

    // Callbacks de RecognitionListener
    override fun onReadyForSpeech(params: Bundle?) {
        _engineState.value = SpeechState.Listening
    }

    override fun onBeginningOfSpeech() {
        beginOfSpeechAtMs = SystemClock.elapsedRealtime()
        _engineState.value = SpeechState.Processing
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        endOfSpeechAtMs = SystemClock.elapsedRealtime()
        _engineState.value = SpeechState.Processing
    }

    /** Marcas de tiempo del enunciado en curso (evaluación de latencia, tesis HE4). Se limpian en cada resultado final. */
    private var beginOfSpeechAtMs: Long? = null
    private var firstPartialAtMs: Long? = null
    private var endOfSpeechAtMs: Long? = null

    private fun clearUtteranceTimestamps() {
        beginOfSpeechAtMs = null
        firstPartialAtMs = null
        endOfSpeechAtMs = null
    }

    override fun onError(error: Int) {
        val errorDescription = getErrorDescription(error)
        Log.w(TAG, "SpeechRecognizer error: $error ($errorDescription) con ${currentProfile().label}")
        clearUtteranceTimestamps() // el enunciado fallido no debe contaminar la latencia del siguiente

        if (!isContinuousListeningEnabled) {
            _engineState.value = SpeechState.Error(errorDescription, error)
            return
        }

        when (val action = policy.onError(error)) {
            RecognitionFallbackPolicy.Action.Restart -> {
                _engineState.value = SpeechState.Error(errorDescription, error)
                scheduleRestart(delayMs = 350, recreate = false)
            }
            RecognitionFallbackPolicy.Action.Recreate -> {
                // Error transitorio del cliente: no se muestra al usuario, se reintenta con un reconocedor nuevo
                scheduleRestart(delayMs = 500, recreate = true)
            }
            is RecognitionFallbackPolicy.Action.SwitchProfile -> {
                val profile = profiles[action.index]
                if (error == ERROR_LANGUAGE_NOT_SUPPORTED || error == ERROR_LANGUAGE_UNAVAILABLE) {
                    runLanguageDiagnostics()
                }
                Log.w(TAG, "Cambiando de modo de reconocimiento a: ${profile.label}")
                scheduleRestart(delayMs = 300, recreate = true)
            }
            RecognitionFallbackPolicy.Action.GiveUp -> {
                isContinuousListeningEnabled = false
                gaveUp = true
                restartJob?.cancel()
                _modeNotice.value = "No se pudo iniciar el reconocimiento de voz ($errorDescription). " +
                    "Revisa que la app «Google» y los «Servicios de voz de Google» estén actualizados y que el " +
                    "español esté instalado en Ajustes > Idioma > Voz."
                _engineState.value = SpeechState.Error(errorDescription, error)
                _engineState.value = SpeechState.Idle
            }
        }
    }

    override fun onResults(results: Bundle?) {
        val resultsAtMs = SystemClock.elapsedRealtime()
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidencesArray = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        // Evidencia ASR-02 (tesis): confianza cruda tal como la entrega la API, antes de normalizarla.
        // Filtrar con: adb logcat -s AndroidSpeechEngine:I | grep ASR02
        Log.i(TAG, "ASR02 perfil=${currentProfile().label} n=${matches?.size ?: 0} " +
            "confianzas=${confidencesArray?.joinToString(prefix = "[", postfix = "]") ?: "null"} " +
            "texto=\"${matches?.firstOrNull().orEmpty()}\"")

        if (!matches.isNullOrEmpty()) {
            val recognizedText = matches[0].trim()
            if (recognizedText.isNotBlank()) {
                val words = recognizedText.split(Regex("\\s+")).filter { it.isNotBlank() }
                val defaultConfidence = resolveConfidence(confidencesArray?.firstOrNull())
                val wordConfidences = words.map { defaultConfidence }

                _engineState.value = SpeechState.FinalText(recognizedText, wordConfidences)

                val result = RawTranscriptionResult(
                    rawText = recognizedText,
                    words = words,
                    wordConfidences = wordConfidences,
                    isFinal = true,
                    rawConfidences = confidencesArray?.toList(),
                    asrProfile = currentProfile().label,
                    beginOfSpeechAtMs = beginOfSpeechAtMs,
                    firstPartialAtMs = firstPartialAtMs,
                    endOfSpeechAtMs = endOfSpeechAtMs,
                    resultsAtMs = resultsAtMs,
                    sequence = resultSequence.incrementAndGet()
                )
                scope.launch { _transcriptionStream.emit(result) }
            }
        }
        clearUtteranceTimestamps()

        policy.onSuccess()

        // Auto-reinicio continuo (un único reinicio pendiente)
        if (isContinuousListeningEnabled) {
            scheduleRestart(delayMs = 150, recreate = false)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        policy.onSuccess() // un parcial demuestra que el perfil actual funciona
        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!partialMatches.isNullOrEmpty()) {
            val partialText = partialMatches[0].trim()
            if (partialText.isNotBlank()) {
                if (firstPartialAtMs == null) firstPartialAtMs = SystemClock.elapsedRealtime()
                _engineState.value = SpeechState.PartialText(partialText)
                val words = partialText.split(Regex("\\s+")).filter { it.isNotBlank() }
                val wordConfidences = words.map { 0.60f }
                val sequence = resultSequence.incrementAndGet()
                scope.launch {
                    _transcriptionStream.emit(
                        RawTranscriptionResult(
                            rawText = partialText,
                            words = words,
                            wordConfidences = wordConfidences,
                            isFinal = false,
                            sequence = sequence
                        )
                    )
                }
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun destroy() {
        isContinuousListeningEnabled = false
        restartJob?.cancel()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error al destruir speechRecognizer: ${e.message}")
        }
        speechRecognizer = null
    }

    private fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Error de captura de micrófono"
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente ASR de Android"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Faltan permisos de micrófono"
            SpeechRecognizer.ERROR_NETWORK -> "Error de conexión ASR"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera agotado"
            SpeechRecognizer.ERROR_NO_MATCH -> "Pausa o silencio en la voz"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Servicio ASR ocupado, reiniciando..."
            SpeechRecognizer.ERROR_SERVER -> "Error en el servidor de voz"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silencio continuo detectado"
            ERROR_LANGUAGE_NOT_SUPPORTED -> "Idioma español no soportado por el reconocedor del equipo"
            ERROR_LANGUAGE_UNAVAILABLE -> "Falta el paquete de voz en español sin conexión (descárgalo en Ajustes > Voz)"
            else -> "Aviso ASR (Código $errorCode)"
        }
    }

    companion object {
        private const val TAG = "AndroidSpeechEngine"
        const val OFFLINE_LANGUAGE_TAG = "es-US"
        const val ONLINE_LANGUAGE_TAG = "es-419"

        // SpeechRecognizer.ERROR_LANGUAGE_* existen desde API 31; minSdk es 26, se usan los literales.
        const val ERROR_LANGUAGE_NOT_SUPPORTED = RecognitionFallbackPolicy.ERROR_LANGUAGE_NOT_SUPPORTED
        const val ERROR_LANGUAGE_UNAVAILABLE = RecognitionFallbackPolicy.ERROR_LANGUAGE_UNAVAILABLE

        /** Confianza neutra cuando el motor no reporta puntuación utilizable. */
        const val NEUTRAL_CONFIDENCE = 0.65f

        /**
         * Normaliza la confianza reportada por el ASR. Muchos motores devuelven -1 (o 0.0) para
         * "desconocida"; antes se recortaba a 0.0, lo que forzaba confianza "Baja" en todas las
         * palabras y disparaba correcciones difusas indebidas.
         */
        fun resolveConfidence(raw: Float?): Float {
            if (raw == null || raw.isNaN() || raw <= 0.0f) return NEUTRAL_CONFIDENCE
            return raw.coerceIn(0.0f, 1.0f)
        }
    }
}
