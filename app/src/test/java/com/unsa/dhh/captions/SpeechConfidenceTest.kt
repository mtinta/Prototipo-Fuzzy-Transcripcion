package com.unsa.dhh.captions

import com.unsa.dhh.captions.domain.asr.AndroidSpeechEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/** Regresión: confianza "desconocida" del ASR (-1 / 0.0 / ausente) no debe leerse como confianza 0. */
class SpeechConfidenceTest {
    private val neutral = AndroidSpeechEngine.NEUTRAL_CONFIDENCE

    @Test fun missingOrUnknown_becomesNeutral() {
        assertEquals(neutral, AndroidSpeechEngine.resolveConfidence(null), 0f)
        assertEquals(neutral, AndroidSpeechEngine.resolveConfidence(-1f), 0f)
        assertEquals(neutral, AndroidSpeechEngine.resolveConfidence(0f), 0f)
        assertEquals(neutral, AndroidSpeechEngine.resolveConfidence(Float.NaN), 0f)
    }

    @Test fun validValues_preservedAndClamped() {
        assertEquals(0.42f, AndroidSpeechEngine.resolveConfidence(0.42f), 0f)
        assertEquals(1.0f, AndroidSpeechEngine.resolveConfidence(1.7f), 0f)
    }
}
