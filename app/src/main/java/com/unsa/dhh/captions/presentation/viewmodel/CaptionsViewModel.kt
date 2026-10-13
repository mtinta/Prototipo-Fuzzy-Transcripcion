package com.unsa.dhh.captions.presentation.viewmodel

import android.app.Application
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
import com.unsa.dhh.captions.data.telemetry.SessionLogManager
import com.unsa.dhh.captions.data.telemetry.SessionStats
import com.unsa.dhh.captions.domain.asr.AndroidSpeechEngine
import com.unsa.dhh.captions.domain.asr.RawTranscriptionResult
import com.unsa.dhh.captions.domain.asr.SpeechState
import com.unsa.dhh.captions.presentation.NotesExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID

/**
 * ViewModel principal del prototipo de subtitulado accesible.
 * Coordina la cadena completa: ASR -> PLN -> Inferencia Mamdani -> Room DB -> Teleprompter.
 */
class CaptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppRoomDatabase.getDatabase(application)
    private val dictionaryDao = database.dictionaryDao()
    private val sessionLogDao = database.sessionLogDao()

    private val speechEngine = AndroidSpeechEngine(application)
    private val decisionEvaluator = DecisionEvaluator()
    private val sessionLogManager = SessionLogManager(sessionLogDao, viewModelScope)
    private val notesExporter = NotesExporter(application)

    // Estado de la sesión actual
    private var currentSessionId = UUID.randomUUID().toString()

    private val _selectedSubject = MutableStateFlow("BIOLOGIA")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

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

    private val _asrStatusMessage = MutableStateFlow<String?>(null)
    val asrStatusMessage: StateFlow<String?> = _asrStatusMessage.asStateFlow()

    // Acumuladores de transcripción permanente para evitar duplicidad de texto
    private val committedLines = mutableListOf<String>()
    private var activePartialLine: String = ""

    // Diccionario en memoria rápida de la asignatura activa
    private var activeDictionary: List<AcademicTerm> = emptyList()

    init {
        loadDictionaryForSubject(_selectedSubject.value)
        observeSpeechEngine()
    }

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
        loadDictionaryForSubject(subject)
    }

    private fun loadDictionaryForSubject(subject: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var terms = dictionaryDao.getTermsBySubject(subject)
            if (terms.isEmpty()) {
                terms = loadFallbackDictionaryFromAssets(subject)
                if (terms.isNotEmpty()) {
                    try {
                        dictionaryDao.insertAll(terms)
                    } catch (e: Exception) {
                        Log.w("CaptionsViewModel", "No se pudo insertar diccionario en Room DB: ${e.message}")
                    }
                }
            }
            activeDictionary = terms
            Log.i("CaptionsViewModel", "Diccionario activo para $subject: ${activeDictionary.size} términos.")
        }
    }

    private fun loadFallbackDictionaryFromAssets(subject: String): List<AcademicTerm> {
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

    private fun observeSpeechEngine() {
        viewModelScope.launch {
            speechEngine.transcriptionStream.collect { rawResult ->
                processIncomingRawText(rawResult)
            }
        }

        viewModelScope.launch {
            speechEngine.engineState.collect { state ->
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
        val words = rawResult.words
        val confidences = rawResult.wordConfidences
        if (words.isEmpty()) return

        viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()

            val correctionResults = decisionEvaluator.processSentence(
                rawWords = words,
                confidences = confidences,
                dictionary = activeDictionary
            )

            val latencyMs = System.currentTimeMillis() - startTime
            val correctedSentence = correctionResults.joinToString(" ") { it.finalWord }

            if (rawResult.isFinal) {
                sessionLogManager.recordBatch(
                    sessionId = currentSessionId,
                    subject = _selectedSubject.value,
                    results = correctionResults,
                    latencyMs = latencyMs
                )

                if (correctedSentence.isNotBlank()) {
                    committedLines.add(correctedSentence)
                }
                activePartialLine = ""
                _recentCorrections.value = correctionResults.filter { it.wasSubstituted }
                _sessionStats.value = sessionLogManager.getSessionStats()
            } else {
                activePartialLine = correctedSentence
                val subsInPartial = correctionResults.filter { it.wasSubstituted }
                if (subsInPartial.isNotEmpty()) {
                    sessionLogManager.recordBatch(
                        sessionId = currentSessionId,
                        subject = _selectedSubject.value,
                        results = subsInPartial,
                        latencyMs = latencyMs
                    )
                    _sessionStats.value = sessionLogManager.getSessionStats()
                }
            }

            val currentAllLines = if (activePartialLine.isBlank()) {
                committedLines.toList()
            } else {
                committedLines.toList() + activePartialLine
            }

            _teleprompterLines.value = currentAllLines
            _correctedTranscriptText.value = currentAllLines.joinToString("\n")
            _rawTranscriptText.value = currentAllLines.joinToString("\n")
        }
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

    fun clearAsrStatusMessage() {
        _asrStatusMessage.value = null
    }

    fun exportNotesAsTxt() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = notesExporter.exportToTxt(_selectedSubject.value, _correctedTranscriptText.value)
            _lastExportMessage.value = if (file != null) "Apuntes TXT guardados: ${file.name}" else "Error al guardar TXT"
        }
    }

    fun exportNotesAsPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = notesExporter.exportToPdf(_selectedSubject.value, _correctedTranscriptText.value)
            _lastExportMessage.value = if (file != null) "Apuntes PDF guardados: ${file.name}" else "Error al guardar PDF"
        }
    }

    fun clearExportMessage() {
        _lastExportMessage.value = null
    }

    fun clearTranscription() {
        committedLines.clear()
        activePartialLine = ""
        _rawTranscriptText.value = ""
        _correctedTranscriptText.value = ""
        _teleprompterLines.value = emptyList()
        currentSessionId = UUID.randomUUID().toString()
        sessionLogManager.resetSession()
        _sessionStats.value = SessionStats(0, 0, 0L, 0.0, 0.0)
        _asrStatusMessage.value = null
    }

    override fun onCleared() {
        speechEngine.destroy()
        super.onCleared()
    }
}
