package com.unsa.dhh.captions.data.telemetry

import java.io.File

/**
 * Dónde se guardan los datos de evaluación.
 *
 * Android 11+ (API 30): carpeta PÚBLICA `Documentos/PrototipoFuzzy/`, visible con cualquier explorador de archivos y
 * por USB, y que NO se borra al desinstalar la app. Android deja crear archivos en `Documents/` sin pedir permisos.
 * Android 8–10: carpeta privada `Android/data/<paquete>/files/evaluacion/` (la pública exigiría permisos).
 *
 *     PrototipoFuzzy/
 *       sus_respuestas.csv, nasa_tlx_respuestas.csv          (acumulan a todos los participantes)
 *       sesion_<fecha>_<participante>_<condicion>/           (UNA carpeta por sesión)
 *         sesion_..._<condicion>.csv  + _meta.txt            (enunciados: UtteranceLogger)
 *         sesion_..._<condicion>_a01.wav, _a02.wav…          (grabación: SessionAudioRecorder)
 *         indice_audio.csv                                   (inicio de cada WAV)
 *
 * Ojo: si se DESINSTALA la app y se vuelve a instalar, Android ya no la deja abrir los archivos de la instalación
 * anterior (p. ej. `nasa_tlx_respuestas.csv`). Antes de reinstalar, renombrar la carpeta `PrototipoFuzzy`.
 */
object EvaluationStorage {

    const val APP_FOLDER = "PrototipoFuzzy"
    /** Carpeta privada (Android 8–10, y la usada por las versiones anteriores de la app). */
    const val PRIVATE_FOLDER = "evaluacion"

    private val SESSION_CSV = Regex("""(sesion_.+)\.csv""")
    private val SESSION_META = Regex("""(sesion_.+)_meta\.txt""")
    private val SESSION_WAV = Regex("""(sesion_.+)_a\d+\.wav""")

    /** [publicDocuments] = `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS)`, o null si no se puede usar. */
    fun chooseDir(publicDocuments: File?, privateBase: File): File {
        if (publicDocuments != null) {
            val dir = File(publicDocuments, APP_FOLDER)
            if (isWritable(dir)) return dir
        }
        return File(privateBase, PRIVATE_FOLDER)
    }

    private fun isWritable(dir: File): Boolean = try {
        dir.mkdirs()
        val probe = File(dir, ".prueba_escritura_${System.nanoTime()}")
        probe.writeText("ok")
        probe.delete()
        true
    } catch (_: Exception) {
        false
    }

    /**
     * Copia a [to], ya ordenado en una carpeta por sesión, lo que las versiones anteriores guardaron en [from]
     * (todo junto, con los WAV en `audio/`). No sobrescribe nada ni borra el original (queda como respaldo); se puede
     * llamar en cada arranque. [wavs] = false copia solo CSV y textos (rápido); true, solo los WAV.
     * Devuelve cuántos archivos copió.
     */
    fun migrateLegacy(from: File, to: File, wavs: Boolean): Int {
        if (!from.isDirectory || from.canonicalPath == to.canonicalPath) return 0
        var copied = 0
        val audio = File(from, "audio")
        if (wavs) {
            audio.listFiles()?.forEach { f ->
                val session = SESSION_WAV.matchEntire(f.name)?.groupValues?.get(1) ?: return@forEach
                if (copyIfAbsent(f, File(File(to, session), f.name))) copied++
            }
            return copied
        }
        from.listFiles()?.filter { it.isFile }?.forEach { f ->
            val session = SESSION_META.matchEntire(f.name)?.groupValues?.get(1)
                ?: SESSION_CSV.matchEntire(f.name)?.groupValues?.get(1)
            val dst = if (session != null) File(File(to, session), f.name) else File(to, f.name)
            if (copyIfAbsent(f, dst)) copied++
        }
        copied += splitAudioIndex(File(audio, SessionAudioRecorder.INDEX_FILE), to)
        return copied
    }

    /** El índice viejo tenía todas las sesiones juntas: cada carpeta de sesión recibe solo sus filas. */
    private fun splitAudioIndex(index: File, to: File): Int {
        if (!index.isFile) return 0
        val lines = try { index.readLines(Charsets.UTF_8) } catch (_: Exception) { return 0 }
        if (lines.isEmpty()) return 0
        val col = lines.first().split(",").indexOf("sesion").takeIf { it >= 0 } ?: return 0
        var written = 0
        lines.drop(1).filter { it.isNotBlank() }
            // Los nombres de sesión no llevan comas ni comillas (UtteranceLogger.sanitizeForFileName)
            .groupBy { it.split(",").getOrNull(col)?.trim('"') ?: "" }
            .filterKeys { it.startsWith("sesion_") }
            .forEach { (session, rows) ->
                val dst = File(File(to, session), SessionAudioRecorder.INDEX_FILE)
                if (dst.exists()) return@forEach
                try {
                    dst.parentFile?.mkdirs()
                    dst.writeText((listOf(lines.first()) + rows).joinToString("\n", postfix = "\n"), Charsets.UTF_8)
                    written++
                } catch (_: Exception) {
                }
            }
        return written
    }

    private fun copyIfAbsent(src: File, dst: File): Boolean {
        if (dst.exists()) return false
        return try {
            dst.parentFile?.mkdirs()
            val tmp = File(dst.parentFile, dst.name + ".copiando")
            src.copyTo(tmp, overwrite = true)
            tmp.renameTo(dst) || run { tmp.delete(); false }
        } catch (_: Exception) {
            false // sin espacio o sin permiso: el original sigue en la carpeta privada
        }
    }
}
