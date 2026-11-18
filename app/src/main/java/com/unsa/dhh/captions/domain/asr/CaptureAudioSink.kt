package com.unsa.dhh.captions.domain.asr

/**
 * Recibe una copia del audio que el motor ASR lee del micrófono (el mismo PCM que reconoce Vosk).
 * Lo usa la grabación del docente (data/telemetry/SessionAudioRecorder).
 *
 * Se llama desde el hilo de captura: las implementaciones no deben bloquear ni lanzar excepciones.
 */
interface CaptureAudioSink {
    /**
     * [samples] PCM 16 bits mono a [sampleRate] Hz; solo valen las primeras [count] muestras (el búfer se reutiliza:
     * hay que copiarlo). [readAtMs] = `SystemClock.elapsedRealtime()` justo después de la lectura (≈ última muestra).
     */
    fun onAudio(samples: ShortArray, count: Int, sampleRate: Int, readAtMs: Long)

    /** La captura terminó (detención, cambio de micrófono o fallo): el próximo audio abre un tramo nuevo. */
    fun onCaptureEnded()
}
