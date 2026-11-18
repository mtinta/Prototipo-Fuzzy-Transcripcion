package com.unsa.dhh.captions

import com.unsa.dhh.captions.data.decision.WordCorrectionResult
import com.unsa.dhh.captions.data.telemetry.UtteranceLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class UtteranceLoggerTest {

    private val info = UtteranceLogger.SessionInfo(
        sessionId = "s1", participant = "E01", label = "piloto", condition = "T0_SIN_CORRECCION",
        subject = "BIOLOGIA", audioSource = "USB", deviceType = "telefono", device = "Test", appVersion = "1.0.0"
    )

    private fun utterance(raw: String, corrected: String) = UtteranceLogger.Utterance(
        index = 1, wallClockMs = 0L, asrProfile = "sin conexión preferido (es-US)",
        rawConfidences = null, confidenceUsed = 0.65f,
        rawText = raw, correctedText = corrected, displayedText = raw,
        beginOfSpeechAtMs = 1000L, firstPartialAtMs = 1400L, endOfSpeechAtMs = 3000L, resultsAtMs = 3300L,
        correctionDoneAtMs = 3302L, publishedAtMs = 3303L, correctionMs = 1.5, audioSource = "USB", textScale = 1.2f,
        words = listOf(
            WordCorrectionResult("la", "la", 0.65, 0.0, 0.0, false),
            WordCorrectionResult("cédula", "célula", 0.65, 0.83, 0.77, true, 0.67)
        )
    )

    /** Separa una línea CSV respetando comillas (suficiente para verificar el escapado). */
    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoted && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { out.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    @Test
    fun rowHasOneValuePerHeaderColumnAndKeepsBothTexts() {
        val row = UtteranceLogger.buildRow(info, utterance("la cédula, vegetal", "la célula, vegetal"))
        val values = parseCsvLine(row)
        val h = UtteranceLogger.HEADER
        assertEquals(h.size, values.size)
        assertEquals("la cédula, vegetal", values[h.indexOf("texto_crudo")])
        assertEquals("la célula, vegetal", values[h.indexOf("texto_corregido")])
        assertEquals("NA", values[h.indexOf("confianza_api")])
        assertEquals("1", values[h.indexOf("n_sustituciones")])
        assertEquals("303", values[h.indexOf("ms_fin_voz_a_publicado")])
        assertEquals("400", values[h.indexOf("ms_inicio_voz_a_primer_parcial")])
        assertEquals("cédula>célula|0.650|0.830|0.670|0.770|S", values[h.indexOf("palabras_evaluadas")])
        assertEquals("telefono", values[h.indexOf("tipo_dispositivo")])
        assertEquals("1.2", values[h.indexOf("escala_texto")])
    }

    @Test
    fun csvEscapesQuotesAndCommas() {
        assertEquals("\"dijo \"\"hola\"\", luego\"", UtteranceLogger.csv("dijo \"hola\", luego"))
        assertEquals("simple", UtteranceLogger.csv("simple"))
    }

    @Test
    fun writesSessionFileAndSusFile() {
        val dir = Files.createTempDirectory("evaluacion").toFile()
        val logger = UtteranceLogger(dir)
        logger.startSession(info)
        logger.log(utterance("la cedula", "la célula"))
        logger.logSus("E01", "piloto", "tableta", listOf(4, 2, 4, 1, 5, 2, 4, 1, 5, 2), listOf(5, 4, 4, 2))
        logger.flush()

        // Una carpeta por sesión, con el CSV y su _meta.txt dentro; el SUS queda fuera (acumula participantes)
        val sessionDir = dir.listFiles { f -> f.isDirectory && f.name.startsWith("sesion_") }!!.single()
        assertEquals(sessionDir, logger.currentSessionDir)
        // La carpeta lleva la etiqueta; el CSV conserva el nombre sin ella (lo usan el índice de audio y el análisis)
        assertTrue(sessionDir.name, Regex("sesion_\\d{8}_\\d{6}_piloto_E01_T0_SIN_CORRECCION").matches(sessionDir.name))
        val session = java.io.File(sessionDir, sessionDir.name.replace("_piloto_", "_") + ".csv")
        assertTrue(session.exists())
        assertEquals(session.nameWithoutExtension, logger.currentBaseName)
        assertTrue(java.io.File(sessionDir, session.nameWithoutExtension + "_meta.txt").exists())
        val lines = session.readLines()
        assertEquals(2, lines.size)
        assertEquals(UtteranceLogger.HEADER.joinToString(","), lines[0])

        val sus = java.io.File(dir, "sus_respuestas.csv").readLines()
        assertEquals(2, sus.size)
        assertEquals(UtteranceLogger.SUS_HEADER.joinToString(","), sus[0])
        val susRow = parseCsvLine(sus[1])
        assertEquals(UtteranceLogger.SUS_HEADER.size, susRow.size)
        assertEquals("4", susRow[UtteranceLogger.SUS_HEADER.indexOf("item1")])
        assertTrue("la app no calcula el puntaje", "sus" !in UtteranceLogger.SUS_HEADER)
        assertEquals("tableta", susRow[UtteranceLogger.SUS_HEADER.indexOf("tipo_dispositivo")])
        assertEquals("2", susRow[UtteranceLogger.SUS_HEADER.indexOf("comp_distraccion")])
        dir.deleteRecursively()
    }

    @Test
    fun tlxRowsKeepConditionAndCountOrderPerParticipant() {
        val dir = Files.createTempDirectory("evaluacion").toFile()
        val logger = UtteranceLogger(dir)
        logger.logTlx("E01", "clase1", "telefono", "T2", listOf(50, 10, 30, 20, 40, 0))
        logger.logTlx("E02", "clase1", "telefono", "T0", listOf(60, 20, 30, 30, 50, 10))
        logger.logTlx("E01", "clase1", "telefono", "T0", listOf(70, 10, 50, 40, 60, 30))
        logger.flush()

        val lines = java.io.File(dir, UtteranceLogger.TLX_FILE).readLines()
        val h = UtteranceLogger.TLX_HEADER
        assertEquals(h.joinToString(","), lines[0])
        val rows = lines.drop(1).map { parseCsvLine(it) }
        assertEquals(3, rows.size)
        assertEquals(listOf("T2", "T0", "T0"), rows.map { it[h.indexOf("condicion")] })
        assertEquals(listOf("1", "1", "2"), rows.map { it[h.indexOf("orden")] })
        assertEquals(listOf("70", "10", "50", "40", "60", "30"), rows[2].takeLast(6))
        assertTrue("la app no calcula el puntaje", "tlx_raw" !in h)
        dir.deleteRecursively()
    }

    @Test
    fun oldSusFileWithAnotherHeaderIsRenamedNotMixed() {
        val dir = Files.createTempDirectory("evaluacion").toFile()
        val old = java.io.File(dir, UtteranceLogger.SUS_FILE)
        old.writeText("fecha,participante,etiqueta,tipo_dispositivo,item1,item2,item3,item4,item5,item6,item7,item8,item9,item10,sus\n" +
            "2026-11-01 10:00:00,E09,piloto,telefono,4,2,4,1,5,2,4,1,5,2,85.0\n")
        val logger = UtteranceLogger(dir)
        logger.logSus("E01", "piloto", "tableta", List(10) { 3 }, listOf(3, 3, 3, 3))
        logger.flush()

        assertEquals(UtteranceLogger.SUS_HEADER.joinToString(","), old.readLines()[0])
        assertEquals(2, old.readLines().size)
        val renamed = dir.listFiles { f -> f.name.startsWith("sus_respuestas_formato_anterior_") }!!.single()
        assertTrue(renamed.readText().contains("E09"))
        dir.deleteRecursively()
    }

    @Test
    fun sinEtiquetaLaCarpetaNoLaLlevaYDosSesionesSeguidasNoSeMezclan() {
        val dir = Files.createTempDirectory("evaluacion").toFile()
        val logger = UtteranceLogger(dir)
        logger.startSession(info.copy(label = "  "))
        val first = logger.currentSessionDir!!
        logger.startSession(info.copy(label = "  ")) // mismo segundo, misma identidad
        val second = logger.currentSessionDir!!
        logger.flush()

        assertTrue(first.name, Regex("sesion_\\d{8}_\\d{6}_E01_T0_SIN_CORRECCION").matches(first.name))
        assertTrue(java.io.File(first, first.name + ".csv").exists())
        if (second.name != first.name) { // si cambió el segundo entre ambas, no hay sufijo
            assertTrue(java.io.File(second, second.name + ".csv").exists())
        }
        assertEquals(2, dir.listFiles { f -> f.isDirectory }!!.size)
    }
}
