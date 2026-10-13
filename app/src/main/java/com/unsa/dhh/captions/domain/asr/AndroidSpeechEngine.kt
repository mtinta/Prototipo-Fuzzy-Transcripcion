package com.unsa.dhh.captions.domain.asr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
class AndroidSpeechEngine(private val context: Context) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isContinuousListeningEnabled = false

    private val _engineState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val engineState: StateFlow<SpeechState> = _engineState.asStateFlow()

    private val _transcriptionStream = MutableSharedFlow<RawTranscriptionResult>(replay = 0)
    val transcriptionStream: SharedFlow<RawTranscriptionResult> = _transcriptionStream.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "SpeechRecognizer no está disponible en este dispositivo.")
            _engineState.value = SpeechState.Error("Reconocimiento de voz no disponible en el sistema", -1)
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error al limpiar el reconocedor previo: ${e.message}")
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@AndroidSpeechEngine)
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo instanciar SpeechRecognizer: ${e.message}", e)
            _engineState.value = SpeechState.Error("Error al iniciar el motor ASR nativo", -1)
            return
        }

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }
    }

    /**
     * Inicia la escucha continua de la voz del docente.
     */
    fun startListening() {
        isContinuousListeningEnabled = true
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
    fun stopListening() {
        isContinuousListeningEnabled = false
        try {
            speechRecognizer?.stopListening()
            _engineState.value = SpeechState.Idle
            Log.d(TAG, "SpeechRecognizer detenido por el usuario.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener escucha: ${e.message}", e)
        }
    }

    // Callbacks de RecognitionListener
    override fun onReadyForSpeech(params: Bundle?) {
        _engineState.value = SpeechState.Listening
    }

    override fun onBeginningOfSpeech() {
        _engineState.value = SpeechState.Processing
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _engineState.value = SpeechState.Processing
    }

    override fun onError(error: Int) {
        val errorDescription = getErrorDescription(error)
        Log.w(TAG, "SpeechRecognizer error: $error ($errorDescription)")

        _engineState.value = SpeechState.Error(errorDescription, error)

        // Re-intento continuo tras silencios o errores
        if (isContinuousListeningEnabled) {
            scope.launch {
                delay(350)
                if (isContinuousListeningEnabled) {
                    try {
                        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            error == SpeechRecognizer.ERROR_CLIENT ||
                            error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            initializeRecognizer()
                        } else {
                            speechRecognizer?.cancel()
                        }
                        speechRecognizer?.startListening(recognizerIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al auto-reiniciar ASR: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidencesArray = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

        if (!matches.isNullOrEmpty()) {
            val recognizedText = matches[0].trim()
            if (recognizedText.isNotBlank()) {
                val words = recognizedText.split(Regex("\\s+")).filter { it.isNotBlank() }
                val defaultConfidence = confidencesArray?.firstOrNull() ?: 0.65f
                val wordConfidences = words.map { defaultConfidence.coerceIn(0.0f, 1.0f) }

                _engineState.value = SpeechState.FinalText(recognizedText, wordConfidences)

                scope.launch {
                    _transcriptionStream.emit(
                        RawTranscriptionResult(
                            rawText = recognizedText,
                            words = words,
                            wordConfidences = wordConfidences,
                            isFinal = true
                        )
                    )
                }
            }
        }

        // Auto-reinicio continuo
        if (isContinuousListeningEnabled) {
            scope.launch {
                delay(150)
                if (isContinuousListeningEnabled) {
                    try {
                        speechRecognizer?.startListening(recognizerIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al reanudar escucha continua: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!partialMatches.isNullOrEmpty()) {
            val partialText = partialMatches[0].trim()
            if (partialText.isNotBlank()) {
                _engineState.value = SpeechState.PartialText(partialText)
                val words = partialText.split(Regex("\\s+")).filter { it.isNotBlank() }
                val wordConfidences = words.map { 0.60f }
                scope.launch {
                    _transcriptionStream.emit(
                        RawTranscriptionResult(
                            rawText = partialText,
                            words = words,
                            wordConfidences = wordConfidences,
                            isFinal = false
                        )
                    )
                }
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        isContinuousListeningEnabled = false
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
            else -> "Aviso ASR (Código $errorCode)"
        }
    }

    companion object {
        private const val TAG = "AndroidSpeechEngine"
    }
}
