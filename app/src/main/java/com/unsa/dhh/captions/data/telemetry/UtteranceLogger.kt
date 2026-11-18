package com.unsa.dhh.captions.data.telemetry

import com.unsa.dhh.captions.data.decision.WordCorrectionResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Registro experimental POR ENUNCIADO para la evaluación de la tesis (protocolo, Sección 7 del plan 2.0).
 *
 * Cada enunciado final del reconocedor produce una fila con:
 *  - el texto crudo del ASR (condición T0) y el texto corregido por la capa difusa (T2), SIEMPRE ambos,
 *    aunque la pantalla muestre solo uno (interruptor "sin corrección"); así se comparan exactamente las
 *    mismas transcripciones antes y después del procesamiento difuso;
 *  - lo que vio el estudiante en pantalla (según la condición activa);
 *  - las marcas de tiempo del enunciado para medir la latencia (reloj monotónico, ms);
 *  - la confianza cruda del ASR (Android: una por alternativa; Vosk: una por palabra) y la C media usada;
 *  - el detalle de cada palabra evaluada por el motor difuso (C, D, S, y*, decisión).
 *
 * El archivo se escribe en vivo (una fila por enunciado, con flush), de modo que sobrevive a un cierre
 * inesperado. Se escribe en un hilo propio para no retrasar el teleprompter.
 *
 * Una CARPETA por sesión dentro de [baseDir] (ver [EvaluationStorage]), con el CSV, su `_meta.txt` y la grabación:
 *
 *     sesion_<fecha>_<etiqueta>_<participante>_<condicion>/sesion_<fecha>_<participante>_<condicion>.csv
 *
 * La carpeta lleva además la etiqueta de la sesión (piloto, clase1…; se omite si está vacía). Los archivos
 * conservan el nombre sin etiqueta: es el que usan el índice de audio y evaluar_sesiones.py.
 *
 * El SUS y el NASA-TLX acumulan a todos los participantes: quedan en [baseDir], fuera de las carpetas de sesión.
 * Descarga: adb pull /sdcard/Documents/PrototipoFuzzy/ .
 */
class UtteranceLogger(
    private val baseDir: File,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
) {

    data class SessionInfo(
        val sessionId: String,
        val participant: String,
        val label: String,
        val condition: String,
        val subject: String,
        val audioSource: String,
        /** "telefono" o "tableta" (DeviceClass). */
        val deviceType: String,
        val device: String,
        val appVersion: String
    )

    data class Utterance(
        val index: Int,
        val wallClockMs: Long,
        val asrProfile: String,
        val rawConfidences: List<Float>?,
        val confidenceUsed: Float,
        val rawText: String,
        val correctedText: String,
        val displayedText: String,
        val beginOfSpeechAtMs: Long?,
        val firstPartialAtMs: Long?,
        val endOfSpeechAtMs: Long?,
        val resultsAtMs: Long?,
        val correctionDoneAtMs: Long,
        val publishedAtMs: Long,
        val correctionMs: Double,
        val audioSource: String,
        /** Escala del tamaño de letra elegida por el estudiante (1.0 = tamaño base). */
        val textScale: Float = 1.0f,
        val words: List<WordCorrectionResult>
    )

    @Volatile
    private var currentFile: File? = null
    @Volatile
    private var session: SessionInfo? = null

    val currentFilePath: String? get() = currentFile?.absolutePath

    /** La carpeta se crea en el hilo del registro: esto evita repetir nombre antes de que exista en disco. */
    @Volatile
    private var previousStem: String? = null

    /** Nombre del CSV en curso sin extensión: la grabación de audio usa el mismo nombre (SessionAudioRecorder). */
    val currentBaseName: String? get() = currentFile?.nameWithoutExtension

    /** Carpeta de la sesión en curso: ahí van también los WAV de la grabación. */
    val currentSessionDir: File? get() = currentFile?.parentFile

    /** Abre una carpeta y un archivo nuevos para la sesión. Una sesión = una condición experimental. */
    fun startSession(info: SessionInfo) {
        session = info
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        val safeParticipant = sanitizeForFileName(info.participant.ifBlank { "SINCODIGO" })
        val safeLabel = sanitizeForFileName(info.label.trim())
        val fileStem = "sesion_${stamp}_${safeParticipant}_${info.condition}"
        val dirStem = if (safeLabel.isEmpty()) fileStem
        else "sesion_${stamp}_${safeLabel}_${safeParticipant}_${info.condition}"
        // Dos sesiones abiertas en el mismo segundo (T0 → T2 seguidos): nunca mezclar ni sobrescribir
        var suffix = ""
        var n = 1
        while (File(baseDir, dirStem + suffix).exists() || dirStem + suffix == previousStem) suffix = "_${++n}"
        previousStem = dirStem + suffix
        val dir = File(baseDir, dirStem + suffix)
        val file = File(dir, "$fileStem$suffix.csv")
        currentFile = file
        executor.execute {
            dir.mkdirs()
            file.writeText(HEADER.joinToString(",") + "\n", Charsets.UTF_8)
            File(dir, file.nameWithoutExtension + "_meta.txt").writeText(
                buildString {
                    appendLine("session_id=${info.sessionId}")
                    appendLine("participante=${info.participant}")
                    appendLine("etiqueta=${info.label}")
                    appendLine("condicion=${info.condition}")
                    appendLine("asignatura=${info.subject}")
                    appendLine("fuente_audio_inicial=${info.audioSource}")
                    appendLine("tipo_dispositivo=${info.deviceType}")
                    appendLine("equipo=${info.device}")
                    appendLine("version_app=${info.appVersion}")
                    appendLine("inicio=${Date()}")
                },
                Charsets.UTF_8
            )
        }
    }

    fun log(u: Utterance) {
        val info = session ?: return
        val file = currentFile ?: return
        val line = buildRow(info, u)
        executor.execute { file.appendText(line + "\n", Charsets.UTF_8) }
    }

    /**
     * Guarda las respuestas del SUS y de los 4 ítems complementarios (un archivo acumulativo para todos los
     * participantes). Se guardan tal como se marcaron (1-5), sin puntaje: el SUS y la inversión del ítem de
     * distracción se calculan en el análisis, no en la app.
     */
    fun logSus(participant: String, label: String, deviceType: String, responses: List<Int>, complementary: List<Int>) {
        require(responses.size == SUS_ITEMS) { "El SUS tiene $SUS_ITEMS ítems" }
        require(complementary.size == SUS_COMPLEMENTARY_HEADER.size) { "Faltan ítems complementarios" }
        val row = listOf(timestamp(), participant, label, deviceType) +
            responses.map { it.toString() } + complementary.map { it.toString() }
        appendRow(SUS_FILE, SUS_HEADER, row)
    }

    /**
     * Guarda un NASA-TLX Raw (una fila por mitad de la clase). [condition] es "T0" o "T2": se toma de la
     * condición activa y no se muestra al estudiante. El orden (1 = primera mitad respondida) se calcula con
     * las filas ya guardadas del mismo participante y etiqueta. Sin puntaje: la media se calcula en el análisis.
     */
    fun logTlx(participant: String, label: String, deviceType: String, condition: String, values: List<Int>) {
        require(values.size == TLX_SUBSCALES) { "El NASA-TLX tiene $TLX_SUBSCALES subescalas" }
        val stamp = timestamp()
        executor.execute {
            val file = File(baseDir, TLX_FILE)
            val previous = if (file.exists()) {
                file.readLines(Charsets.UTF_8).drop(1).count { line ->
                    val cols = line.split(",")
                    cols.size > 2 && cols[1] == csv(participant) && cols[2] == csv(label)
                }
            } else 0
            val row = listOf(stamp, participant, label, deviceType, condition, (previous + 1).toString()) +
                values.map { it.toString() }
            writeRow(file, TLX_HEADER, row)
        }
    }

    private fun timestamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())

    private fun appendRow(fileName: String, header: List<String>, row: List<String>) {
        executor.execute { writeRow(File(baseDir, fileName), header, row) }
    }

    /**
     * Escribe una fila (en el hilo del registro). Si el archivo existe con otra cabecera (versión anterior de la
     * app), se renombra en vez de mezclar columnas distintas en el mismo archivo.
     */
    private fun writeRow(file: File, header: List<String>, row: List<String>) {
        baseDir.mkdirs()
        val headerLine = header.joinToString(",")
        if (file.exists() && file.useLines { it.firstOrNull() } != headerLine) {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
            file.renameTo(File(baseDir, "${file.nameWithoutExtension}_formato_anterior_$stamp.csv"))
        }
        if (!file.exists()) file.writeText(headerLine + "\n", Charsets.UTF_8)
        file.appendText(row.joinToString(",") { csv(it) } + "\n", Charsets.UTF_8)
    }

    /** Espera a que se escriba todo lo pendiente (pruebas y cierre de sesión). */
    fun flush() {
        executor.submit {}.get()
    }

    companion object {
        const val SUS_FILE = "sus_respuestas.csv"
        const val TLX_FILE = "nasa_tlx_respuestas.csv"
        const val SUS_ITEMS = 10
        const val TLX_SUBSCALES = 6

        /** Ítems complementarios del Anexo A (1-5). comp_distraccion es inverso. */
        val SUS_COMPLEMENTARY_HEADER = listOf("comp_legibilidad", "comp_velocidad", "comp_comodidad", "comp_distraccion")

        val SUS_HEADER = listOf("fecha", "participante", "etiqueta", "tipo_dispositivo") +
            (1..SUS_ITEMS).map { "item$it" } + SUS_COMPLEMENTARY_HEADER

        /** tlx_1..6 = demanda mental, física, temporal, desempeño (0 = perfecto), esfuerzo, frustración. */
        val TLX_HEADER = listOf("fecha", "participante", "etiqueta", "tipo_dispositivo", "condicion", "orden") +
            (1..TLX_SUBSCALES).map { "tlx_$it" }

        val HEADER = listOf(
            "session_id", "participante", "etiqueta", "condicion", "asignatura", "tipo_dispositivo", "idx", "hora",
            "perfil_asr", "fuente_audio", "escala_texto", "confianza_api", "c_usada",
            "texto_crudo", "texto_corregido", "texto_mostrado", "n_palabras", "n_sustituciones",
            "t_inicio_voz", "t_primer_parcial", "t_fin_voz", "t_resultado", "t_correccion", "t_publicado",
            "ms_capa_difusa", "ms_fin_voz_a_publicado", "ms_inicio_voz_a_primer_parcial",
            "palabras_evaluadas"
        )

        /** Formato de la columna palabras_evaluadas: original>final|C|D|S|y*|S/N ; separadas por " ; ". */
        fun formatEvaluatedWords(words: List<WordCorrectionResult>): String =
            words.filter { it.lexicalSimilarity >= 0.50 || it.wasSubstituted }
                .joinToString(" ; ") {
                    String.format(
                        Locale.ROOT, "%s>%s|%.3f|%.3f|%.3f|%.3f|%s",
                        it.originalWord, it.finalWord, it.acousticConfidence, it.lexicalSimilarity, it.semanticCoherence,
                        it.fuzzyJustification, if (it.wasSubstituted) "S" else "N"
                    )
                }

        fun buildRow(info: SessionInfo, u: Utterance): String {
            fun diff(a: Long?, b: Long?): String = if (a != null && b != null) (a - b).toString() else ""
            val values = listOf(
                info.sessionId, info.participant, info.label, info.condition, info.subject, info.deviceType,
                u.index.toString(),
                SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(Date(u.wallClockMs)),
                u.asrProfile, u.audioSource, String.format(Locale.ROOT, "%.1f", u.textScale),
                u.rawConfidences?.joinToString(" ") { String.format(Locale.ROOT, "%.4f", it) } ?: "NA",
                String.format(Locale.ROOT, "%.3f", u.confidenceUsed),
                u.rawText, u.correctedText, u.displayedText,
                u.words.size.toString(), u.words.count { it.wasSubstituted }.toString(),
                u.beginOfSpeechAtMs?.toString().orEmpty(), u.firstPartialAtMs?.toString().orEmpty(),
                u.endOfSpeechAtMs?.toString().orEmpty(), u.resultsAtMs?.toString().orEmpty(),
                u.correctionDoneAtMs.toString(), u.publishedAtMs.toString(),
                String.format(Locale.ROOT, "%.3f", u.correctionMs),
                diff(u.publishedAtMs, u.endOfSpeechAtMs),
                diff(u.firstPartialAtMs, u.beginOfSpeechAtMs),
                formatEvaluatedWords(u.words)
            )
            return values.joinToString(",") { csv(it) }
        }

        /** Escapado CSV (RFC 4180): comillas dobles si hay coma, comilla o salto de línea. */
        fun csv(value: String): String =
            if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else value

        fun sanitizeForFileName(s: String): String = s.replace(Regex("[^A-Za-z0-9_-]"), "_").take(24)
    }
}
