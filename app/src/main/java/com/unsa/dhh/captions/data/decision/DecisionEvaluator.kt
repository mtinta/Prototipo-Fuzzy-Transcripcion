package com.unsa.dhh.captions.data.decision

import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.domain.fuzzy.JFuzzyEngine
import com.unsa.dhh.captions.domain.fuzzy.LevenshteinMatcher
import com.unsa.dhh.captions.domain.nlp.OpenNlpPosTagger
import java.util.Locale

/**
 * Resultado de evaluación y corrección difusa de una palabra.
 */
data class WordCorrectionResult(
    val originalWord: String,
    val finalWord: String,
    val acousticConfidence: Double,
    val lexicalSimilarity: Double,
    val fuzzyJustification: Double,
    val wasSubstituted: Boolean
)

/**
 * Módulo 4: Evaluador de Decisión y Sustitución Silenciosa.
 * Aplica el umbral nítido de decisión y* >= 0.65.
 * Si la justificación calculada por Mamdani supera el umbral, sustituye silenciosamente
 * el término dudoso por la forma canónica del diccionario escolar.
 */
class DecisionEvaluator(
    private val posTagger: OpenNlpPosTagger = OpenNlpPosTagger(),
    private val levenshteinMatcher: LevenshteinMatcher = LevenshteinMatcher(),
    private val fuzzyEngine: JFuzzyEngine = JFuzzyEngine()
) {

    /**
     * Umbral nítido de decisión especificado en la tesis (y* >= 0.65).
     */
    val decisionThreshold: Double = 0.65

    /**
     * Procesa una secuencia de palabras transcritas contra el diccionario en memoria de la materia activa.
     */
    fun processSentence(
        rawWords: List<String>,
        confidences: List<Float>,
        dictionary: List<AcademicTerm>
    ): List<WordCorrectionResult> {
        val results = mutableListOf<WordCorrectionResult>()

        for (i in rawWords.indices) {
            val originalWord = rawWords[i]
            val confidence = (confidences.getOrNull(i) ?: 0.55f).toDouble()

            // Si es conector, artículo o palabra vacía, se conserva sin alteración
            if (!posTagger.isCandidateNoun(originalWord) || dictionary.isEmpty()) {
                results.add(
                    WordCorrectionResult(
                        originalWord = originalWord,
                        finalWord = originalWord,
                        acousticConfidence = confidence,
                        lexicalSimilarity = 0.0,
                        fuzzyJustification = 0.0,
                        wasSubstituted = false
                    )
                )
                continue
            }

            // Buscar el término más próximo en el diccionario académico
            var bestTerm: AcademicTerm? = null
            var bestSimilarity = 0.0

            for (term in dictionary) {
                val sim = levenshteinMatcher.calculateCombinedSimilarity(originalWord, term.term)
                if (sim > bestSimilarity) {
                    bestSimilarity = sim
                    bestTerm = term
                }
            }

            // Si hay un término candidato, evaluamos con el motor difuso de Mamdani
            if (bestTerm != null && bestSimilarity >= 0.50) {
                val fuzzyScore = fuzzyEngine.evaluate(confidence, bestSimilarity)

                if (fuzzyScore >= decisionThreshold) {
                    // Sustitución silenciosa aprobada
                    // Respetar mayúscula inicial si la palabra original la tenía
                    val preservedCaseWord = matchCase(originalWord, bestTerm.term)
                    results.add(
                        WordCorrectionResult(
                            originalWord = originalWord,
                            finalWord = preservedCaseWord,
                            acousticConfidence = confidence,
                            lexicalSimilarity = bestSimilarity,
                            fuzzyJustification = fuzzyScore,
                            wasSubstituted = true
                        )
                    )
                } else {
                    // No superó el umbral: se conserva la palabra original del ASR
                    results.add(
                        WordCorrectionResult(
                            originalWord = originalWord,
                            finalWord = originalWord,
                            acousticConfidence = confidence,
                            lexicalSimilarity = bestSimilarity,
                            fuzzyJustification = fuzzyScore,
                            wasSubstituted = false
                        )
                    )
                }
            } else {
                results.add(
                    WordCorrectionResult(
                        originalWord = originalWord,
                        finalWord = originalWord,
                        acousticConfidence = confidence,
                        lexicalSimilarity = bestSimilarity,
                        fuzzyJustification = 0.0,
                        wasSubstituted = false
                    )
                )
            }
        }

        return results
    }

    private fun matchCase(source: String, target: String): String {
        return if (source.firstOrNull()?.isUpperCase() == true) {
            target.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            target
        }
    }
}
