package com.unsa.dhh.captions.domain.asr

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Módulo 2: contrato común de los motores ASR.
 *
 * El ViewModel solo conoce esta interfaz, así se puede cambiar de reconocedor (Vosk / Android) sin tocar
 * la cadena PLN -> Mamdani -> teleprompter. Cada motor entrega [RawTranscriptionResult] con la confianza
 * acústica C por palabra en `wordConfidences`.
 */
interface SpeechEngine {
    /** Estado del reconocedor (escuchando, error, detenido...). */
    val engineState: StateFlow<SpeechState>

    /** Resultados parciales y finales, en orden de llegada. */
    val transcriptionStream: SharedFlow<RawTranscriptionResult>

    /** Aviso informativo para el usuario (cambio de modo, carga del modelo...). null = sin aviso. */
    val modeNotice: StateFlow<String?>

    /** Número del último resultado (parcial o final) producido; sirve para descartar parciales superados. */
    fun latestResultSequence(): Long

    fun startListening()
    fun stopListening()

    /** El micrófono activo cambió (p. ej. se conectó el receptor USB): reabrir la captura con la fuente nueva. */
    fun restartForAudioSourceChange()

    fun destroy()
}

/** Motores disponibles. Vosk es el predeterminado: entrega C por palabra (ver evaluation/vosk_fase0). */
enum class AsrEngineType(val displayName: String) {
    VOSK("Vosk (sin conexión, C por palabra)"),
    ANDROID("Android SpeechRecognizer (C fija 0.65)")
}
