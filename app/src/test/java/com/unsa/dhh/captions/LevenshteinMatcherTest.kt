package com.unsa.dhh.captions

import com.unsa.dhh.captions.domain.fuzzy.LevenshteinMatcher
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para distancia de Levenshtein y normalización fonética hispana.
 */
class LevenshteinMatcherTest {

    private lateinit var matcher: LevenshteinMatcher

    @Before
    fun setUp() {
        matcher = LevenshteinMatcher()
    }

    @Test
    fun testCedulaVsCelula_highCombinedSimilarity() {
        // En aula escolar con seseo o ruido, "cédula" sustituye a "célula"
        val sim = matcher.calculateCombinedSimilarity("cedula", "célula")
        println("cedula vs célula -> Similitud: $sim")
        assertTrue("La similitud debe ser >= 0.70", sim >= 0.70)
    }

    @Test
    fun testFotosintesisAccents() {
        val sim = matcher.calculateCombinedSimilarity("fotosintesis", "fotosíntesis")
        println("fotosintesis vs fotosíntesis -> Similitud: $sim")
        assertTrue("La similitud debe ser 1.0 tras normalización de tildes", sim >= 0.99)
    }

    @Test
    fun testTawantinsuyoPhonetics() {
        val sim = matcher.calculateCombinedSimilarity("tawantinsuyo", "Tahuantinsuyo")
        println("tawantinsuyo vs Tahuantinsuyo -> Similitud: $sim")
        assertTrue("La similitud debe ser >= 0.85", sim >= 0.85)
    }
}
