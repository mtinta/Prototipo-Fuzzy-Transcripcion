package com.unsa.dhh.captions

import com.unsa.dhh.captions.domain.fuzzy.JFuzzyEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para el motor de inferencia difusa Mamdani y defuzzificación COG.
 */
class FuzzyEngineTest {

    private lateinit var fuzzyEngine: JFuzzyEngine

    @Before
    fun setUp() {
        fuzzyEngine = JFuzzyEngine()
    }

    @Test
    fun testLowConfidenceHighSimilarity_approvesSubstitution() {
        // Docente en aula ruidosa (baja confianza C = 0.30) pero palabra muy parecida a término técnico (D = 0.92)
        val score = fuzzyEngine.evaluate(confianza = 0.30, similitud = 0.92)
        println("Low Confidence + High Sim -> y* = $score")
        // Debe superar el umbral nítido y* >= 0.65
        assertTrue("La justificación difusa debería ser >= 0.65, pero fue $score", score >= 0.65)
    }

    @Test
    fun testHighConfidenceLowSimilarity_rejectsSubstitution() {
        // Docente dijo una palabra común con alta confianza acústica (C = 0.95), similitud baja (D = 0.20)
        val score = fuzzyEngine.evaluate(confianza = 0.95, similitud = 0.20)
        println("High Confidence + Low Sim -> y* = $score")
        // No debe sustituir
        assertFalse("No debería sustituir, pero fue $score", score >= 0.65)
    }

    @Test
    fun testLowConfidenceLowSimilarity_rejectsSubstitution() {
        // Ruido o palabra extraña no relacionada
        val score = fuzzyEngine.evaluate(confianza = 0.20, similitud = 0.25)
        println("Low Confidence + Low Sim -> y* = $score")
        assertFalse("No debería sustituir ruido inconexo", score >= 0.65)
    }
}
