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

    @Test
    fun testKer_allKeywordsCorrect_zeroKer() {
        val reference = "la célula procariota carece de núcleo definido"
        val hypothesis = "la célula procariota carece de núcleo definido"
        val keywords = setOf("célula", "procariota", "núcleo")

        val ker = sessionLogManager.calculateKer(hypothesis, reference, keywords)
        assertEquals(0.0, ker, 0.001)
    }

    @Test
    fun testKer_halfKeywordsMissing_halfKer() {
        val reference = "la célula tiene mitocondria ribosoma y membrana"
        // Hipótesis pierde "mitocondria" y "membrana", preserva "célula" y "ribosoma" (2 de 4)
        val hypothesis = "la célula tiene motoneta ribosoma y manzana"
        val keywords = setOf("célula", "mitocondria", "ribosoma", "membrana")

        val ker = sessionLogManager.calculateKer(hypothesis, reference, keywords)
        assertEquals(0.50, ker, 0.001)
    }

    @Test
    fun testKer_noKeywordsInHypothesis_fullKer() {
        val reference = "la fotosíntesis ocurre en el cloroplasto"
        // Hipótesis no acierta ninguna palabra clave
        val hypothesis = "la foto síntesis ocurre en el coro plasto"
        val keywords = setOf("fotosíntesis", "cloroplasto")

        val ker = sessionLogManager.calculateKer(hypothesis, reference, keywords)
        assertEquals(1.0, ker, 0.001)
    }

    @Test
    fun testKer_emptyKeywordsInReference_zeroKer() {
        val reference = "el profesor explica la lección en la pizarra"
        val hypothesis = "el profesor habla de la lección"
        val keywords = setOf("célula", "mitocondria") // Ninguna aparece en reference

        val ker = sessionLogManager.calculateKer(hypothesis, reference, keywords)
        assertEquals(0.0, ker, 0.001)
    }

    @Test
    fun testKer_multiWordKeywords_matchedCorrectly() {
        val reference = "el átomo forma un enlace covalente y tiene número atómico"
        val rawHypothesis = "el átomo forma un enlace con valiente y tiene numero"
        val correctedHypothesis = "el átomo forma un enlace covalente y tiene número atómico"
        val keywords = setOf("enlace covalente", "número atómico", "átomo")

        val rawKer = sessionLogManager.calculateKer(rawHypothesis, reference, keywords)
        val correctedKer = sessionLogManager.calculateKer(correctedHypothesis, reference, keywords)

        // Raw: acierta solo "átomo" (1 de 3) -> KER = 1 - 1/3 = 0.6667
        assertEquals(2.0 / 3.0, rawKer, 0.01)
        // Corregido: acierta los 3 -> KER = 0.0
        assertEquals(0.0, correctedKer, 0.001)
        assertTrue("La corrección difusa reduce el KER en términos compuestos", correctedKer < rawKer)
    }

    @Test
    fun testKer_fuzzyCorrection_improvesKer() {
        val reference = "la célula realiza fotosíntesis en el cloroplasto"
        val rawHypothesis = "la cédula realiza fotosintesis en el coroplasto"
        val correctedHypothesis = "la célula realiza fotosíntesis en el cloroplasto"
        val keywords = setOf("célula", "fotosíntesis", "cloroplasto")

        val rawKer = sessionLogManager.calculateKer(rawHypothesis, reference, keywords)
        val correctedKer = sessionLogManager.calculateKer(correctedHypothesis, reference, keywords)

        assertTrue("El KER crudo debe ser mayor que el corregido", rawKer > correctedKer)
        assertEquals(0.0, correctedKer, 0.001)
    }
}
