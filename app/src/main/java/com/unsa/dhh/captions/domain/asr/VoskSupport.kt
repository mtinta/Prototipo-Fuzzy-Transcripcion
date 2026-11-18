package com.unsa.dhh.captions.domain.asr

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlin.math.roundToLong

/** Palabra reconocida por Vosk. `startSec`/`endSec` se cuentan desde que empezó el flujo de audio. */
data class VoskWord(val word: String, val confidence: Float, val startSec: Double, val endSec: Double)

/**
 * Lectura del JSON de Vosk (lógica pura, sin Android: se prueba en JVM).
 *
 *  - Final con `setWords(true)`: {"result":[{"conf":0.62,"start":1.2,"end":1.8,"word":"mitocondria"}...],"text":"..."}
 *  - Parcial: {"partial":"..."}
 *
 * `conf` es la probabilidad posterior de la palabra en el lattice (decodificación MBR). A diferencia de la API de
 * Android, aquí 0.0 es una confianza real (muy baja), no "desconocida".
 */
object VoskResultParser {
    private val gson = Gson()

    private class WordJson(val word: String?, val conf: Double?, val start: Double?, val end: Double?)
    private class ResultJson(val text: String?, val partial: String?, val result: List<WordJson>?)

    private fun read(json: String): ResultJson? = try {
        gson.fromJson(json, ResultJson::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }

    /** Palabras de un resultado final. Si faltara el detalle por palabra, se usa el texto con C neutra. */
    fun parseFinal(json: String): List<VoskWord> {
        val r = read(json) ?: return emptyList()
        val detailed = r.result.orEmpty().filter { !it.word.isNullOrBlank() }
        if (detailed.isNotEmpty()) {
            return detailed.map {
                VoskWord(
                    word = it.word!!.trim(),
                    confidence = toConfidence(it.conf),
                    startSec = it.start ?: 0.0,
                    endSec = it.end ?: it.start ?: 0.0
                )
            }
        }
        return r.text.orEmpty().split(Regex("\\s+")).filter { it.isNotBlank() }
            .map { VoskWord(it, NEUTRAL_CONFIDENCE, 0.0, 0.0) }
    }

    /** Texto de un resultado parcial ("" si no hay voz aún). */
    fun parsePartial(json: String): String = read(json)?.partial.orEmpty().trim()

    fun toConfidence(raw: Double?): Float =
        if (raw == null || raw.isNaN()) NEUTRAL_CONFIDENCE else raw.toFloat().coerceIn(0.0f, 1.0f)

    /** Misma C neutra que el motor de Android, solo para el caso (no esperado) de que falte `conf`. */
    const val NEUTRAL_CONFIDENCE = AndroidSpeechEngine.NEUTRAL_CONFIDENCE
}

/**
 * Convierte un instante del flujo de audio (segundos desde el inicio de la captura, como los reporta Vosk)
 * a `SystemClock.elapsedRealtime()`, para que t_inicio_voz / t_fin_voz usen el mismo reloj que el resto
 * de la telemetría.
 *
 * Se ancla en la ÚLTIMA lectura del micrófono: la última muestra leída ocurrió aproximadamente en `nowMs`.
 * El error es del orden del búfer de AudioRecord (decenas de ms) y no se acumula con el tiempo.
 */
class AudioClock(private val sampleRate: Int) {
    private var samples = 0L
    private var lastReadAtMs = 0L

    fun onRead(sampleCount: Int, nowMs: Long) {
        samples += sampleCount
        lastReadAtMs = nowMs
    }

    val audioSeconds: Double get() = samples.toDouble() / sampleRate

    fun toElapsedRealtime(audioSec: Double): Long =
        lastReadAtMs - ((audioSeconds - audioSec) * 1000.0).roundToLong()
}
