package com.unsa.dhh.captions

import com.unsa.dhh.captions.domain.fuzzy.JFuzzyEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para el motor de inferencia difusa Mamdani tri-variable (C, D, S)
 * y defuzzificación por Centroide de Gravedad (COG).
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
        // Con coherencia semántica neutra por defecto (S = 0.50)
        val score = fuzzyEngine.evaluate(confianza = 0.30, similitud = 0.92)
        println("Low Confidence + High Sim + Neutral Sem -> y* = $score")
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

    @Test
    fun testSemanticContext_boostsJustification() {
        // C = 0.55 (media), D = 0.78 (media/alta)
        // Con semántica alta (S = 0.90) vs semántica baja (S = 0.10)
        val scoreHighSem = fuzzyEngine.evaluate(confianza = 0.55, similitud = 0.78, semantica = 0.90)
        val scoreLowSem = fuzzyEngine.evaluate(confianza = 0.55, similitud = 0.78, semantica = 0.10)

        println("Media Conf + Media/Alta Sim + Alta Sem -> y* = $scoreHighSem")
        println("Media Conf + Media/Alta Sim + Baja Sem -> y* = $scoreLowSem")

        assertTrue("El refuerzo semántico debe producir mayor justificación ($scoreHighSem > $scoreLowSem)", scoreHighSem > scoreLowSem)
        assertTrue("Con contexto semántico alto debe superar el umbral (0.65)", scoreHighSem >= 0.65)
    }

    @Test
    fun testHighConfidence_preservesTeacherSpeechRegardlessOfSemantics() {
        // Si el docente pronunció con alta confianza acústica C = 0.95 (ASR seguro)
        val score = fuzzyEngine.evaluate(confianza = 0.95, similitud = 0.85, semantica = 0.90)
        println("High Confidence + High Sim + High Sem -> y* = $score")
        assertFalse("No debe sustituir palabras con alta confianza acústica (y* < 0.65)", score >= 0.65)
    }
}
