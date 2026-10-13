package com.unsa.dhh.captions.domain.fuzzy

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Módulo 3: Coincidencia aproximada de cadenas y similitud fonético-léxica.
 * Calcula la distancia de Levenshtein normalizada D in [0.0, 1.0] e integra
 * normalización fonética hispana para neutralizar el seseo, yeísmo, confusión b/v
 * y la omisión de h muda propia del habla en aulas escolares de Arequipa.
 */
class LevenshteinMatcher {

    /**
     * Calcula la distancia de edición de Levenshtein entre dos cadenas de texto.
     */
    fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1, // Eliminación
                    min(
                        dp[i][j - 1] + 1, // Inserción
                        dp[i - 1][j - 1] + cost // Sustitución
                    )
                )
            }
        }
        return dp[len1][len2]
    }

    /**
     * Calcula la similitud ortográfica normalizada D in [0.0, 1.0].
     * D = 1.0 - (distancia / max(longitudes))
     */
    fun calculateNormalizedSimilarity(s1: String, s2: String): Double {
        val clean1 = stripAccents(s1.lowercase(Locale.ROOT).trim())
        val clean2 = stripAccents(s2.lowercase(Locale.ROOT).trim())

        if (clean1 == clean2) return 1.0
        val maxLength = max(clean1.length, clean2.length)
        if (maxLength == 0) return 1.0

        val distance = calculateLevenshteinDistance(clean1, clean2)
        val similarity = 1.0 - (distance.toDouble() / maxLength.toDouble())
        return similarity.coerceIn(0.0, 1.0)
    }

    /**
     * Transforma una cadena a su representación fonética hispana:
     * - Neutraliza 'b' y 'v' -> 'b'
     * - Neutraliza 'c' (ante e,i), 'z', 's' -> 's'
     * - Neutraliza 'll' y 'y' -> 'y'
     * - Neutraliza 'h' muda
     * - Neutraliza 'qu' y 'k' -> 'k'
     */
    fun toHispanicPhonetic(text: String): String {
        var str = stripAccents(text.lowercase(Locale.ROOT).trim())
        str = str.replace("h", "")
        str = str.replace("v", "b")
        str = str.replace("ce", "se")
        str = str.replace("ci", "si")
        str = str.replace("z", "s")
        str = str.replace("ll", "y")
        str = str.replace("qu", "k")
        str = str.replace("c", "k") // ante a,o,u ya que ce/ci fueron transformadas antes
        return str
    }

    /**
     * Calcula la similitud combinada ponderada: 60% ortográfica normalizada + 40% fonética hispana.
     */
    fun calculateCombinedSimilarity(rawWord: String, dictionaryTerm: String): Double {
        val lexicalSim = calculateNormalizedSimilarity(rawWord, dictionaryTerm)

        val phon1 = toHispanicPhonetic(rawWord)
        val phon2 = toHispanicPhonetic(dictionaryTerm)
        val maxPhonLen = max(phon1.length, phon2.length)
        val phoneticSim = if (maxPhonLen == 0) 1.0 else {
            val dist = calculateLevenshteinDistance(phon1, phon2)
            (1.0 - dist.toDouble() / maxPhonLen.toDouble()).coerceIn(0.0, 1.0)
        }

        // Similitud final D en rango [0.0, 1.0]
        val combined = (0.60 * lexicalSim) + (0.40 * phoneticSim)
        return combined.coerceIn(0.0, 1.0)
    }

    private fun stripAccents(s: String): String {
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
