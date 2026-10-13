package com.unsa.dhh.captions

import com.unsa.dhh.captions.data.telemetry.SessionLogManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para el algoritmo de cálculo formal de WER: WER = (S + D + I) / N
 */
class WerCalculatorTest {

    private lateinit var sessionLogManager: SessionLogManager

    @Before
    fun setUp() {
        sessionLogManager = SessionLogManager(null)
    }

    @Test
    fun testExactMatch_zeroWer() {
        val reference = "la célula vegetal tiene mitocondria"
        val hypothesis = "la célula vegetal tiene mitocondria"
        val wer = sessionLogManager.calculateWer(hypothesis, reference)
        assertEquals(0.0, wer, 0.001)
    }

    @Test
    fun testOneSubstitution_correctWer() {
        val reference = "la célula vegetal tiene mitocondria"
        val hypothesis = "la cédula vegetal tiene mitocondria" // 1 sustitución en 5 palabras = 1/5 = 0.20 (20%)
        val wer = sessionLogManager.calculateWer(hypothesis, reference)
        assertEquals(0.20, wer, 0.001)
    }

    @Test
    fun testWerReductionWithCorrection() {
        val reference = "la célula realiza fotosíntesis en el cloroplasto"
        val rawHypothesis = "la cédula realiza fotosintesis en el coroplasto" // 2 errores léxicos
        val correctedHypothesis = "la célula realiza fotosíntesis en el cloroplasto" // Corregido por capa difusa

        val rawWer = sessionLogManager.calculateWer(rawHypothesis, reference)
        val correctedWer = sessionLogManager.calculateWer(correctedHypothesis, reference)

        println("Raw WER: $rawWer, Corrected WER: $correctedWer")
        assertTrue("El WER corregido debe ser menor que el WER crudo", correctedWer < rawWer)
        assertEquals(0.0, correctedWer, 0.001)
    }
}
