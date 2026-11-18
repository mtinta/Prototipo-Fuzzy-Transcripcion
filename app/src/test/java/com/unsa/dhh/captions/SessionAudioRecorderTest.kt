package com.unsa.dhh.captions

import com.unsa.dhh.captions.data.telemetry.SessionAudioRecorder
import com.unsa.dhh.captions.data.telemetry.WavFileWriter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class SessionAudioRecorderTest {

    private val rate = 16000
    private fun tempDir(): File = Files.createTempDirectory("audio").toFile()

    private fun le(bytes: ByteArray) = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    /** Comprueba la cabecera PCM 16 bits mono y devuelve las muestras del bloque data. */
    private fun readWav(file: File): ShortArray {
        val b = file.readBytes()
        val h = le(b)
        assertEquals("RIFF", String(b, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(b, 8, 4, Charsets.US_ASCII))
        assertEquals("data", String(b, 36, 4, Charsets.US_ASCII))
        assertEquals(1, h.getShort(20).toInt())        // PCM
        assertEquals(1, h.getShort(22).toInt())        // mono
        assertEquals(rate, h.getInt(24))
        assertEquals(16, h.getShort(34).toInt())       // bits
        val dataLen = h.getInt(40)
        assertEquals(b.size - 44, dataLen)
        assertEquals(36 + dataLen, h.getInt(4))
        return ShortArray(dataLen / 2) { h.getShort(44 + it * 2) }
    }

    private fun indexRows(dir: File): List<List<String>> =
        File(dir, SessionAudioRecorder.INDEX_FILE).readLines().drop(1).map { it.split(",") }

    @Test
    fun wavWriterKeepsSamplesInOrderAndHeaderIsValidBeforeClose() {
        val file = File(tempDir(), "t.wav")
        val w = WavFileWriter(file, rate, headerUpdateBytes = 4)
        w.write(shortArrayOf(1, -2, Short.MAX_VALUE, Short.MIN_VALUE), 3) // solo cuentan 3
        // Sin close (cierre inesperado de la app): la cabecera ya refleja lo escrito
        assertArrayEquals(shortArrayOf(1, -2, Short.MAX_VALUE), readWav(file))
        w.write(shortArrayOf(7))
        w.close()
        assertArrayEquals(shortArrayOf(1, -2, Short.MAX_VALUE, 7), readWav(file))
        assertEquals(4L, w.sampleCount)
    }

    @Test
    fun recordsCaptureIntoWavNamedAfterSessionAndIndexesFirstSampleTime() {
        val dir = tempDir()
        val r = SessionAudioRecorder(dir, minFreeBytes = 0, wallClock = { 1_000_000L })
        r.startSession("sesion_20261118_090000_E01_T0_SIN_CORRECCION", "s1")
        val buf = ShortArray(1600) { it.toShort() } // 100 ms
        r.onAudio(buf, 1600, rate, readAtMs = 50_100L)
        r.onAudio(buf, 800, rate, readAtMs = 50_150L)
        r.flush()
        assertEquals("sesion_20261118_090000_E01_T0_SIN_CORRECCION_a01.wav", r.status.value.activeFile)
        r.onCaptureEnded()
        r.flush()
        assertNull(r.status.value.activeFile)

        val samples = readWav(File(dir, "sesion_20261118_090000_E01_T0_SIN_CORRECCION_a01.wav"))
        assertEquals(2400, samples.size)
        assertEquals(799.toShort(), samples[2399])

        val row = indexRows(dir).single()
        val h = SessionAudioRecorder.INDEX_HEADER
        assertEquals("s1", row[h.indexOf("session_id")])
        assertEquals("1", row[h.indexOf("tramo")])
        // Primera muestra = lectura (50 100 ms) − duración del búfer (100 ms): mismo reloj que t_inicio_voz
        assertEquals("50000", row[h.indexOf("t_inicio_audio")])
        assertEquals("16000", row[h.indexOf("frecuencia_hz")])
    }

    @Test
    fun newSegmentAfterCaptureRestartAndNewFileAfterSessionChange() {
        val dir = tempDir()
        val r = SessionAudioRecorder(dir, minFreeBytes = 0)
        val buf = ShortArray(160)
        r.startSession("sesion_A", "a")
        r.onAudio(buf, 160, rate, 1000L)
        r.onCaptureEnded()                    // p. ej. se conectó el receptor SX26
        r.onAudio(buf, 160, rate, 2000L)
        r.startSession("sesion_B", "b")       // cambio T0 → T2 sin detener el micrófono
        r.onAudio(buf, 160, rate, 3000L)
        r.close()

        assertEquals(
            listOf("sesion_A_a01.wav", "sesion_A_a02.wav", "sesion_B_a01.wav"),
            indexRows(dir).map { it[0] }
        )
        listOf("sesion_A_a01.wav", "sesion_A_a02.wav", "sesion_B_a01.wav").forEach {
            assertEquals(160, readWav(File(dir, it)).size)
        }
    }

    @Test
    fun neverOverwritesAnExistingRecordingWithTheSameName() {
        val dir = tempDir()
        val r = SessionAudioRecorder(dir, minFreeBytes = 0)
        r.startSession("sesion_X", "1")
        r.onAudio(ShortArray(10) { 5 }, 10, rate, 1000L)
        r.startSession("sesion_X", "2")       // dos sesiones abiertas en el mismo segundo
        r.onAudio(ShortArray(20), 20, rate, 2000L)
        r.close()
        assertEquals(10, readWav(File(dir, "sesion_X_a01.wav")).size)
        assertEquals(20, readWav(File(dir, "sesion_X_a02.wav")).size)
    }

    @Test
    fun disabledRecordsNothing() {
        val dir = tempDir()
        val r = SessionAudioRecorder(dir, minFreeBytes = 0)
        r.setEnabled(false)
        r.startSession("sesion_A", "a")
        r.onAudio(ShortArray(160), 160, rate, 1000L)
        r.close()
        assertTrue(dir.listFiles().orEmpty().isEmpty())
        assertFalse(r.status.value.enabled)
    }

    @Test
    fun silenceIsRecordedContinuouslyWithoutWarnings() {
        val dir = tempDir()
        val r = SessionAudioRecorder(dir, minFreeBytes = 0)
        r.startSession("sesion_A", "a")
        val zeros = ShortArray(rate)                                   // 1 s de silencio digital
        repeat(30) { r.onAudio(zeros, rate, rate, 1000L + it * 1000L) } // 30 s sin sonido
        r.flush()
        assertEquals("sesion_A_a01.wav", r.status.value.activeFile)    // sigue grabando en el mismo tramo
        assertNull(r.status.value.error)
        r.close()
        assertEquals(30 * rate, readWav(File(dir, "sesion_A_a01.wav")).size)
        assertEquals(1, indexRows(dir).size)
    }

    @Test
    fun lowStorageStopsRecordingWithNoticeAndKeepsWhatWasRecorded() {
        val dir = tempDir()
        var free = Long.MAX_VALUE
        val r = SessionAudioRecorder(dir, minFreeBytes = 1000, freeBytes = { free })
        r.startSession("sesion_A", "a")
        val second = ShortArray(rate)
        r.onAudio(second, rate, rate, 1000L)
        r.flush()
        free = 10
        repeat(60) { r.onAudio(second, rate, rate, 2000L + it * 1000L) } // a los 60 s se revisa el espacio
        r.onAudio(second, rate, rate, 99_000L)                           // ya no se graba
        r.flush()
        assertNull(r.status.value.activeFile)
        assertNotNull(r.status.value.error)
        assertEquals(60 * rate, readWav(File(dir, "sesion_A_a01.wav")).size)
        assertEquals(1, indexRows(dir).size)
        r.close()
    }

    @Test
    fun eachSessionRecordsIntoItsOwnFolderWithItsOwnIndex() {
        val root = tempDir()
        val a = File(root, "sesion_A")
        val b = File(root, "sesion_B")
        val r = SessionAudioRecorder(root, minFreeBytes = 0)
        r.startSession("sesion_A", "a", a)
        r.onAudio(ShortArray(1600), 1600, rate, 1_000L)
        r.startSession("sesion_B", "b", b)
        r.onAudio(ShortArray(1600), 1600, rate, 2_000L)
        r.close()

        assertTrue(File(a, "sesion_A_a01.wav").exists())
        assertTrue(File(b, "sesion_B_a01.wav").exists())
        assertEquals(listOf("sesion_A"), indexRows(a).map { it[SessionAudioRecorder.INDEX_HEADER.indexOf("sesion")] })
        assertEquals(listOf("sesion_B"), indexRows(b).map { it[SessionAudioRecorder.INDEX_HEADER.indexOf("sesion")] })
        assertFalse("nada suelto en la raíz", File(root, SessionAudioRecorder.INDEX_FILE).exists())
    }
}
