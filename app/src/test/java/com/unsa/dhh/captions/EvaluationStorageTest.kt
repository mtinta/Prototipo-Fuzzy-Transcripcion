package com.unsa.dhh.captions

import com.unsa.dhh.captions.data.telemetry.EvaluationStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EvaluationStorageTest {

    private fun tempDir(): File = Files.createTempDirectory("eval").toFile().apply { deleteOnExit() }

    @Test
    fun usaDocumentosPublicosSiSePuedeEscribir() {
        val docs = tempDir()
        val dir = EvaluationStorage.chooseDir(docs, tempDir())
        assertEquals(File(docs, "PrototipoFuzzy"), dir)
        assertTrue(dir.isDirectory)
        assertTrue("no deja el archivo de prueba", dir.listFiles()!!.isEmpty())
    }

    @Test
    fun sinCarpetaPublicaUsaLaPrivada() {
        val priv = tempDir()
        assertEquals(File(priv, "evaluacion"), EvaluationStorage.chooseDir(null, priv))
        // "Documentos" que en realidad es un archivo: no se puede escribir dentro
        val notADir = File(tempDir(), "docs").apply { writeText("x") }
        assertEquals(File(priv, "evaluacion"), EvaluationStorage.chooseDir(notADir, priv))
    }

    @Test
    fun ordenaLoAnteriorEnUnaCarpetaPorSesionSinSobrescribirNiBorrar() {
        val old = tempDir()
        File(old, "audio").mkdirs()
        File(old, "sesion_A_T0.csv").writeText("csv A")
        File(old, "sesion_A_T0_meta.txt").writeText("meta A")
        File(old, "sesion_B_T2.csv").writeText("csv B")
        File(old, "audio/sesion_A_T0_a01.wav").writeBytes(ByteArray(100))
        File(old, "audio/sesion_A_T0_a02.wav").writeBytes(ByteArray(50))
        File(old, "audio/indice_audio.csv").writeText(
            "archivo_wav,sesion,session_id\n" +
                "sesion_A_T0_a01.wav,sesion_A_T0,1\nsesion_B_T2_a01.wav,sesion_B_T2,2\nsesion_A_T0_a02.wav,sesion_A_T0,1\n"
        )
        File(old, "nasa_tlx_respuestas.csv").writeText("viejo")
        val new = tempDir()
        File(new, "nasa_tlx_respuestas.csv").writeText("nuevo")

        // Primero los textos, luego los WAV (como en el ViewModel)
        assertEquals(5, EvaluationStorage.migrateLegacy(old, new, wavs = false)) // 3 textos + 2 índices
        assertFalse(File(new, "sesion_A_T0/sesion_A_T0_a01.wav").exists())
        assertEquals(2, EvaluationStorage.migrateLegacy(old, new, wavs = true))

        val a = File(new, "sesion_A_T0")
        assertEquals("csv A", File(a, "sesion_A_T0.csv").readText())
        assertEquals("meta A", File(a, "sesion_A_T0_meta.txt").readText())
        assertEquals(100, File(a, "sesion_A_T0_a01.wav").length())
        assertEquals(50, File(a, "sesion_A_T0_a02.wav").length())
        assertEquals(
            listOf("archivo_wav,sesion,session_id", "sesion_A_T0_a01.wav,sesion_A_T0,1", "sesion_A_T0_a02.wav,sesion_A_T0,1"),
            File(a, "indice_audio.csv").readLines()
        )
        assertEquals(2, File(new, "sesion_B_T2/indice_audio.csv").readLines().size)
        assertEquals("nuevo", File(new, "nasa_tlx_respuestas.csv").readText())
        assertFalse("sin carpeta audio/ suelta", File(new, "audio").exists())
        assertTrue("el original queda como respaldo", File(old, "audio/sesion_A_T0_a01.wav").exists())

        assertEquals("segunda vez no copia nada", 0,
            EvaluationStorage.migrateLegacy(old, new, false) + EvaluationStorage.migrateLegacy(old, new, true))
        assertFalse(new.walkTopDown().any { it.name.endsWith(".copiando") })
    }
}
