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
 *
 * Las marcas `*AtMs` usan `SystemClock.elapsedRealtime()` (reloj monotónico, ms desde el arranque) para que
 * las latencias no se alteren si cambia la hora del sistema. Son null cuando el reconocedor no emitió el evento.
 */
data class RawTranscriptionResult(
    val rawText: String,
    val words: List<String>,
    val wordConfidences: List<Float>,
    val isFinal: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    /** Confianzas tal como las entregó la API (CONFIDENCE_SCORES), sin normalizar; null si no las entregó. */
    val rawConfidences: List<Float>? = null,
    /** Perfil de reconocimiento usado (local / sin conexión preferido / en línea). */
    val asrProfile: String = "",
    val beginOfSpeechAtMs: Long? = null,
    val firstPartialAtMs: Long? = null,
    val endOfSpeechAtMs: Long? = null,
    val resultsAtMs: Long? = null,
    /** Orden de llegada del resultado (parcial o final). Permite descartar parciales ya superados. */
    val sequence: Long = 0
)
