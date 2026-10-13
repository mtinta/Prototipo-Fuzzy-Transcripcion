package com.unsa.dhh.captions.domain.nlp

import java.util.Locale

/**
 * Módulo 3: Filtro morfosintáctico de Part-of-Speech (POS).
 * Aísla sustantivos y nombres propios clave en español para priorizar
 * la evaluación difusa en la terminología académica de la materia,
 * descartando conectores, artículos y preposiciones.
 */
class OpenNlpPosTagger {

    // Lista de palabras vacías y conectores en español para exclusión inmediata
    private val stopWordsSpanish = hashSetOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "a", "al", "en",
        "con", "por", "para", "sin", "sobre", "tras", "durante", "mediante", "y", "e", "ni",
        "o", "u", "pero", "mas", "sino", "porque", "pues", "ya", "que", "como", "cuando",
        "donde", "quien", "cual", "este", "esta", "estos", "estas", "ese", "esa", "esos",
        "esas", "aquel", "aquella", "aquellos", "aquellas", "su", "sus", "mi", "mis",
        "tu", "tus", "nuestro", "nuestra", "nuestros", "nuestras", "yo", "tu", "el",
        "ella", "nosotros", "ustedes", "ellos", "ellas", "es", "son", "fue", "fueron",
        "era", "eran", "sea", "sean", "muy", "tan", "mas", "menos", "bien", "mal",
        "si", "no", "tambien", "tampoco", "hola", "buenos", "dias", "tardes"
    )

    // Sufijos comunes de sustantivos técnicos y académicos en español
    private val nounSuffixes = listOf(
        "cion", "ción", "sion", "sión", "miento", "dad", "tad", "ismo", "ista",
        "metria", "metría", "grafia", "grafía", "logia", "logía", "soma", "plasto",
        "geno", "geno", "fisis", "lisis", "sis", "trofia", "fera", "fera", "sfera"
    )

    /**
     * Determina si una palabra transcrita es un sustantivo o término técnico candidato.
     * Retorna true si es candidato para cotejar contra el diccionario escolar.
     */
    fun isCandidateNoun(word: String): Boolean {
        val clean = word.lowercase(Locale.ROOT).trim().replace(Regex("[^a-záéíóúüñ]"), "")
        if (clean.length < 3) return false
        if (stopWordsSpanish.contains(clean)) return false

        // Si la primera letra en el texto original era mayúscula, podría ser nombre propio (PROPN)
        if (word.firstOrNull()?.isUpperCase() == true) {
            return true
        }

        // Si termina con sufijo técnico académico
        for (suffix in nounSuffixes) {
            if (clean.endsWith(suffix)) return true
        }

        // Cualquier término con longitud >= 4 no descartado por stopwords es candidato viable
        return clean.length >= 4
    }

    /**
     * Filtra una lista de palabras tokenizadas, devolviendo solo los índices de sustantivos clave.
     */
    fun filterCandidateIndices(words: List<String>): List<Int> {
        val candidates = mutableListOf<Int>()
        for (i in words.indices) {
            if (isCandidateNoun(words[i])) {
                candidates.add(i)
            }
        }
        return candidates
    }
}
