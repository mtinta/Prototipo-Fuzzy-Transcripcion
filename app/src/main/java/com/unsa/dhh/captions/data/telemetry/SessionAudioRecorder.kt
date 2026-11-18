package com.unsa.dhh.captions.data.telemetry

import com.unsa.dhh.captions.domain.asr.CaptureAudioSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

/**
 * Grabación del audio del docente mientras la app escucha, para la transcripción de REFERENCIA (WER/KER) y para
 * volver a pasar el mismo audio por el ASR fuera de línea.
 *
 * Graba exactamente el PCM que recibe Vosk (16 kHz, mono, 16 bits; ≈ 115 MB por hora), en archivos WAV con el
 * mismo nombre que el CSV de la sesión y en la MISMA carpeta de la sesión (ver [UtteranceLogger]):
 *
 *     sesion_<fecha>_<participante>_<condicion>/sesion_<fecha>_<participante>_<condicion>_a01.wav
 *
 * Un TRAMO (a01, a02…) = un periodo de captura continua. Se abre un tramo nuevo al reanudar el micrófono, al
 * cambiar de micrófono (SX26 ↔ teléfono) y al cambiar de sesión (T0 → T2). Cada tramo añade una fila al
 * `indice_audio.csv` de la carpeta de la sesión con `t_inicio_audio`: el instante de su PRIMERA muestra en el mismo reloj
 * (`elapsedRealtime`) que `t_inicio_voz`/`t_fin_voz` del CSV. Así un enunciado está en
 * `t_inicio_voz − t_inicio_audio` ms dentro del WAV (error ≈ tamaño del búfer de lectura, decenas de ms).
 *
 * Todo el trabajo de disco va en un hilo propio: el hilo de captura solo copia el búfer (no se retrasa el ASR).
 * Si queda poco espacio, la grabación se detiene sola (los subtítulos siguen) y se avisa en [status].
 *
 * El silencio se graba igual que la voz: la captura nunca se detiene por falta de sonido.
 */
class SessionAudioRecorder(
    /** Carpeta por defecto (si [startSession] no indica otra); también se usa para medir el espacio libre. */
    private val audioDir: File,
    private val minFreeBytes: Long = DEFAULT_MIN_FREE_BYTES,
    private val freeBytes: (File) -> Long = { it.usableSpace },
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "GrabacionAudio") }
) : CaptureAudioSink {

    data class Status(
        val enabled: Boolean,
        /** Nombre del WAV que se está escribiendo; null = no se está grabando en este momento. */
        val activeFile: String? = null,
        /** Último problema (sin espacio, error de escritura). null = sin problemas. */
        val error: String? = null
    )

    private val _status = MutableStateFlow(Status(enabled = true))
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile private var enabled = true

    // ---- Estado del hilo de grabación (solo se toca dentro de [executor]) ----
    private var baseName: String? = null
    private var sessionDir: File = audioDir
    private var sessionId: String = ""
    private var segment = 0
    private var writer: WavFileWriter? = null
    private var samplesSinceSpaceCheck = 0L
    /** Sin espacio o error de escritura: no se graba más hasta la próxima sesión o hasta reactivar la grabación. */
    private var halted = false

    fun setEnabled(on: Boolean) {
        enabled = on
        submit {
            if (!on) closeSegment()
            halted = false
            _status.value = _status.value.copy(enabled = on, error = null)
        }
    }

    /**
     * Nueva sesión de evaluación: el próximo audio va a `<dir>/<baseName>_a01.wav`. [baseName] = nombre del CSV sin
     * extensión; [dir] = carpeta de la sesión.
     */
    fun startSession(baseName: String, sessionId: String, dir: File = audioDir) {
        submit {
            closeSegment()
            this.baseName = baseName
            this.sessionDir = dir
            this.sessionId = sessionId
            segment = 0
            halted = false
            _status.value = _status.value.copy(error = null)
        }
    }

    override fun onAudio(samples: ShortArray, count: Int, sampleRate: Int, readAtMs: Long) {
        if (!enabled || count <= 0) return
        val copy = samples.copyOf(count.coerceAtMost(samples.size))
        val wallAtRead = wallClock()
        submit { append(copy, sampleRate, readAtMs, wallAtRead) }
    }

    override fun onCaptureEnded() {
        submit { closeSegment() }
    }

    /** Espera a que se escriba lo pendiente (pruebas). */
    fun flush() {
        try {
            executor.submit {}.get()
        } catch (_: RejectedExecutionException) {
        }
    }

    /** Cierra el WAV abierto (cabecera final) y apaga el hilo. */
    fun close() {
        submit { closeSegment() }
        executor.shutdown()
        executor.awaitTermination(2, TimeUnit.SECONDS)
    }

    // ------------------------------------------------------------------ hilo de grabación

    private fun submit(task: () -> Unit) {
        try {
            executor.execute(task)
        } catch (_: RejectedExecutionException) {
            // Ya cerrado (la app se está destruyendo): se descarta
        }
    }

    private fun append(samples: ShortArray, sampleRate: Int, readAtMs: Long, wallAtRead: Long) {
        if (halted || !enabled) return
        val base = baseName ?: return
        try {
            var w = writer
            if (w == null) {
                if (!hasSpace()) return
                // La primera muestra del búfer se capturó `duración del búfer` antes de la lectura
                val bufferMs = samples.size * 1000L / sampleRate
                w = openSegment(base, sampleRate, readAtMs - bufferMs, wallAtRead - bufferMs)
            }
            w.write(samples, samples.size)
            samplesSinceSpaceCheck += samples.size
            if (samplesSinceSpaceCheck >= sampleRate * SPACE_CHECK_EVERY_SEC) {
                samplesSinceSpaceCheck = 0
                hasSpace() // si falta espacio, cierra el tramo y detiene la grabación
            }
        } catch (e: Exception) {
            halt("Error al grabar el audio: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun openSegment(base: String, sampleRate: Int, firstSampleAtMs: Long, firstSampleWallMs: Long): WavFileWriter {
        sessionDir.mkdirs()
        var file: File
        do {
            segment++
            file = File(sessionDir, String.format(Locale.ROOT, "%s_a%02d.wav", base, segment))
        } while (file.exists()) // dos sesiones en el mismo segundo tendrían el mismo nombre: nunca sobrescribir
        val w = WavFileWriter(file, sampleRate)
        writer = w
        samplesSinceSpaceCheck = 0
        appendIndexRow(
            listOf(
                file.name, base, sessionId, segment.toString(), firstSampleAtMs.toString(),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date(firstSampleWallMs)),
                sampleRate.toString()
            )
        )
        _status.value = _status.value.copy(activeFile = file.name, error = null)
        return w
    }

    private fun closeSegment() {
        val w = writer ?: return
        writer = null
        try {
            w.close()
        } catch (_: Exception) {
        }
        // Un tramo sin muestras no sirve: se borra (y se deja su fila en el índice, que es inocua)
        if (w.dataBytes == 0L) w.file.delete()
        _status.value = _status.value.copy(activeFile = null)
    }

    private fun hasSpace(): Boolean {
        val free = try { freeBytes(audioDir.apply { mkdirs() }) } catch (_: Exception) { Long.MAX_VALUE }
        if (free >= minFreeBytes) return true
        halt("Grabación detenida: quedan ${free / (1024 * 1024)} MB libres. Los subtítulos siguen funcionando.")
        return false
    }

    private fun halt(message: String) {
        closeSegment()
        halted = true
        _status.value = _status.value.copy(activeFile = null, error = message)
    }

    private fun appendIndexRow(values: List<String>) {
        val index = File(sessionDir, INDEX_FILE)
        if (!index.exists()) index.writeText(INDEX_HEADER.joinToString(",") + "\n", Charsets.UTF_8)
        index.appendText(values.joinToString(",") { UtteranceLogger.csv(it) } + "\n", Charsets.UTF_8)
    }

    companion object {
        const val INDEX_FILE = "indice_audio.csv"
        val INDEX_HEADER = listOf(
            "archivo_wav", "sesion", "session_id", "tramo", "t_inicio_audio", "hora_inicio", "frecuencia_hz"
        )

        /** Por debajo de esto se deja de grabar (el CSV, la base de datos y el sistema necesitan sitio). */
        const val DEFAULT_MIN_FREE_BYTES = 200L * 1024 * 1024
        private const val SPACE_CHECK_EVERY_SEC = 60
    }
}
