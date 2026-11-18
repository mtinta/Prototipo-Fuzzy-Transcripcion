package com.unsa.dhh.captions

import com.unsa.dhh.captions.domain.asr.AudioClock
import com.unsa.dhh.captions.domain.asr.VoskResultParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lectura del JSON de Vosk y conversión de tiempos del audio al reloj del sistema. */
class VoskSupportTest {

    @Test fun finalResult_keepsPerWordConfidenceAndTimes() {
        // Formato real de vosk 0.3.x con setWords(true)
        val json = """
            {
              "result" : [{"conf" : 1.000000, "end" : 1.52, "start" : 1.11, "word" : "la"},
                          {"conf" : 0.501900, "end" : 2.19, "start" : 1.92, "word" : "mitocondria"}],
              "text" : "la mitocondria"
            }
        """.trimIndent()
        val words = VoskResultParser.parseFinal(json)
        assertEquals(listOf("la", "mitocondria"), words.map { it.word })
        assertEquals(1.0f, words[0].confidence, 1e-6f)
        assertEquals(0.5019f, words[1].confidence, 1e-6f)
        assertEquals(1.92, words[1].startSec, 1e-9)
        assertEquals(2.19, words[1].endSec, 1e-9)
    }

    @Test fun zeroConfidenceIsARealLowValue_notUnknown() {
        // A diferencia de Android, en Vosk 0.0 significa "muy poco probable", no "sin dato"
        val words = VoskResultParser.parseFinal("""{"result":[{"conf":0.0,"start":0,"end":0.3,"word":"sedula"}],"text":"sedula"}""")
        assertEquals(0.0f, words.single().confidence, 0f)
    }

    @Test fun missingConfidence_becomesNeutral_andOutOfRangeIsClamped() {
        assertEquals(VoskResultParser.NEUTRAL_CONFIDENCE, VoskResultParser.toConfidence(null), 0f)
        assertEquals(VoskResultParser.NEUTRAL_CONFIDENCE, VoskResultParser.toConfidence(Double.NaN), 0f)
        assertEquals(1.0f, VoskResultParser.toConfidence(1.3), 0f)
    }

    @Test fun silenceAndMalformedJson_giveNoWords() {
        assertTrue(VoskResultParser.parseFinal("""{"text" : ""}""").isEmpty())
        assertTrue(VoskResultParser.parseFinal("no es json").isEmpty())
        assertEquals("", VoskResultParser.parsePartial("""{"partial" : ""}"""))
    }

    @Test fun finalWithoutWordDetail_fallsBackToTextWithNeutralConfidence() {
        val words = VoskResultParser.parseFinal("""{"text" : "la célula"}""")
        assertEquals(listOf("la", "célula"), words.map { it.word })
        assertTrue(words.all { it.confidence == VoskResultParser.NEUTRAL_CONFIDENCE })
    }

    @Test fun partialText_isTrimmed() {
        assertEquals("la célula vegetal", VoskResultParser.parsePartial("""{"partial" : " la célula vegetal "}"""))
    }

    @Test fun audioClock_anchorsOnLastRead() {
        val clock = AudioClock(16000)
        clock.onRead(16000, nowMs = 11_000L) // 1 s de audio, última muestra leída en t = 11 000 ms
        clock.onRead(8000, nowMs = 11_500L)  // 1.5 s de audio en total
        assertEquals(1.5, clock.audioSeconds, 1e-9)
        assertEquals(11_500L, clock.toElapsedRealtime(1.5)) // la última muestra
        assertEquals(10_000L, clock.toElapsedRealtime(0.0)) // inicio del flujo
        assertEquals(11_200L, clock.toElapsedRealtime(1.2))
    }
}
