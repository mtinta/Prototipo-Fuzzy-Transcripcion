package com.unsa.dhh.captions.domain.fuzzy

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
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
     * Forma precalculada de una palabra: texto limpio (minúsculas, sin tildes) y su transcripción fonética.
     * Calcularla una sola vez evita repetir la normalización en cada una de las ~400 comparaciones.
     */
    class Prepared internal constructor(val clean: String, val phonetic: String)

    /** Caché de los términos del diccionario (son siempre los mismos durante la clase). */
    private val termCache = ConcurrentHashMap<String, Prepared>()

    /** Prepara una palabra del reconocedor (sin caché: cambian en cada enunciado). */
    fun prepare(text: String): Prepared {
        val clean = stripAccents(text.lowercase(Locale.ROOT).trim())
        return Prepared(clean, phoneticOfClean(clean))
    }

    /** Prepara un término del diccionario, reutilizando el cálculo previo si ya se hizo. */
    fun prepareTerm(term: String): Prepared = termCache.getOrPut(term) { prepare(term) }

    /**
     * Calcula la distancia de edición de Levenshtein entre dos cadenas de texto.
     * Usa dos filas en lugar de la matriz completa: mismo resultado, sin reservar memoria por celda.
     */
    fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)

        for (i in 1..len1) {
            curr[0] = i
            val c1 = s1[i - 1]
            for (j in 1..len2) {
                val cost = if (c1 == s2[j - 1]) 0 else 1
                curr[j] = min(
                    prev[j] + 1, // Eliminación
                    min(
                        curr[j - 1] + 1, // Inserción
                        prev[j - 1] + cost // Sustitución
                    )
                )
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[len2]
    }

    /**
     * Calcula la similitud ortográfica normalizada D in [0.0, 1.0].
     * D = 1.0 - (distancia / max(longitudes))
     */
    fun calculateNormalizedSimilarity(s1: String, s2: String): Double =
        normalizedSimilarityOfClean(
            stripAccents(s1.lowercase(Locale.ROOT).trim()),
            stripAccents(s2.lowercase(Locale.ROOT).trim())
        )

    private fun normalizedSimilarityOfClean(clean1: String, clean2: String): Double {
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
    fun toHispanicPhonetic(text: String): String =
        phoneticOfClean(stripAccents(text.lowercase(Locale.ROOT).trim()))

    /** Reglas fonéticas aplicadas a un texto ya limpio (minúsculas, sin tildes). */
    private fun phoneticOfClean(clean: String): String {
        var str = clean
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
    fun calculateCombinedSimilarity(rawWord: String, dictionaryTerm: String): Double =
        combinedSimilarity(prepare(rawWord), prepareTerm(dictionaryTerm))

    /** Igual que [calculateCombinedSimilarity], pero con las dos palabras ya preparadas. */
    fun combinedSimilarity(word: Prepared, term: Prepared): Double {
        val lexicalSim = normalizedSimilarityOfClean(word.clean, term.clean)

        val phon1 = word.phonetic
        val phon2 = term.phonetic
        val maxPhonLen = max(phon1.length, phon2.length)
        val phoneticSim = if (maxPhonLen == 0) 1.0 else {
            val dist = calculateLevenshteinDistance(phon1, phon2)
            (1.0 - dist.toDouble() / maxPhonLen.toDouble()).coerceIn(0.0, 1.0)
        }

        // Similitud final D en rango [0.0, 1.0]
        val combined = (0.60 * lexicalSim) + (0.40 * phoneticSim)
        return combined.coerceIn(0.0, 1.0)
    }

    /**
     * Cota superior EXACTA de [combinedSimilarity] sin calcular Levenshtein: la distancia nunca es menor que la
     * diferencia de largos. Sirve para descartar términos que no pueden llegar a un umbral (mismo resultado).
     */
    fun combinedSimilarityUpperBound(word: Prepared, term: Prepared): Double =
        (0.60 * lengthBound(word.clean, term.clean)) + (0.40 * lengthBound(word.phonetic, term.phonetic))

    private fun lengthBound(a: String, b: String): Double {
        val maxLength = max(a.length, b.length)
        if (maxLength == 0) return 1.0
        return 1.0 - (abs(a.length - b.length).toDouble() / maxLength.toDouble())
    }

    private fun stripAccents(s: String): String {
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
        return normalized.replace(DIACRITICS, "")
    }

    private companion object {
        /** Compilada una sola vez: compilarla en cada llamada era el mayor costo en Android. */
        val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
    }
}
