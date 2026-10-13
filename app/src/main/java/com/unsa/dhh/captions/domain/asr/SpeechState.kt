package com.unsa.dhh.captions.domain.asr

/**
 * Estados del motor de reconocimiento ASR
 */
sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    object Processing : SpeechState()
    data class PartialText(val text: String) : SpeechState()
    data class FinalText(val text: String, val confidences: List<Float>) : SpeechState()
    data class Error(val message: String, val errorCode: Int) : SpeechState()
}

/**
 * Paquete de transcripción primaria entregada por el reconocedor.
 */
data class RawTranscriptionResult(
    val rawText: String,
    val words: List<String>,
    val wordConfidences: List<Float>,
    val isFinal: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
