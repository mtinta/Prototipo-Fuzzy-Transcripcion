package com.unsa.dhh.captions.data.decision

import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import com.unsa.dhh.captions.domain.fuzzy.JFuzzyEngine
import com.unsa.dhh.captions.domain.fuzzy.LevenshteinMatcher
import com.unsa.dhh.captions.domain.nlp.OpenNlpPosTagger
import java.util.Locale

/**
 * Resultado de evaluación y corrección difusa de una palabra.
 * Incluye métricas difusas acústicas, léxicas y semánticas, así como el cluster temático asignado.
 */
data class WordCorrectionResult(
    val originalWord: String,
    val finalWord: String,
    val acousticConfidence: Double,
    val lexicalSimilarity: Double,
    val fuzzyJustification: Double,
    val wasSubstituted: Boolean,
    val semanticCoherence: Double = 0.0,
    val topicCluster: String? = null
)

/**
 * Módulo 4: Evaluador de Decisión y Sustitución Silenciosa.
 * Aplica el umbral nítido de decisión y* >= 0.65.
 *
 * Implementa una arquitectura de dos pasadas:
 * - Pasada 1 (Pre-escaneo de Dominio): Identifica candidatos léxicos preliminares (D >= 0.50)
 *   en toda la oración recibida del ASR.
 * - Pasada 2 (Evaluación Mamdani Tri-Variable): Calcula la Coherencia Semántica (S)
 *   mediante la co-ocurrencia en Topic Clusters de la asignatura y defuzzifica (C, D, S)
 *   obteniendo la justificación nítida y*.
 */
class DecisionEvaluator(
    private val posTagger: OpenNlpPosTagger = OpenNlpPosTagger(),
    private val levenshteinMatcher: LevenshteinMatcher = LevenshteinMatcher(),
    private val fuzzyEngine: JFuzzyEngine = JFuzzyEngine(),
    /** Memoria temática de la clase. null = desactivada (S depende solo de la oración, útil para ablación). */
    val lessonContext: LessonContext? = LessonContext(),
    /**
     * true = la elección entre términos del diccionario parecidos se hace con los TRES factores (C, D, S):
     * se evalúan hasta [MAX_CANDIDATES] candidatos cercanos y gana el de mayor justificación y*.
     * false = comportamiento original: gana siempre el de mayor similitud D (útil para ablación).
     */
    val rerankCandidates: Boolean = true,
    /** Unión de palabras que el ASR partió ("sito plasma" -> citoplasma). Ver [findSplitTerms]. */
    val splitTermMode: SplitTermMode = SplitTermMode.MORPHEME
) {

    /**
     * OFF = palabra por palabra (comportamiento anterior, para ablación).
     * STRICT = no une si un pedazo ya es un término bien escrito.
     * MORPHEME = lo une solo si ese término es parte propia del unido ("plasma" ⊂ "citoplasma").
     */
    enum class SplitTermMode { OFF, STRICT, MORPHEME }

    /** Índice precalculado de clusters (evita reconstruir los conjuntos de términos en cada palabra). */
    private class ClusterIndex(val clusters: List<TopicCluster>) {
        val termSets: List<Set<String>> = clusters.map { it.termsSet }
        /** Área temática de cada cluster (null si no se definió). */
        val macroOf: List<String?> = clusters.map { it.macroId?.takeIf { m -> m.isNotBlank() } }
        val byTerm: Map<String, List<Int>> = HashMap<String, MutableList<Int>>().also { map ->
            termSets.forEachIndexed { i, set -> set.forEach { t -> map.getOrPut(t) { mutableListOf() }.add(i) } }
        }
        /** Áreas temáticas a las que pertenece cada término (a través de sus clusters). */
        val termMacros: Map<String, Set<String>> = byTerm.mapValues { (_, idxs) -> idxs.mapNotNull { macroOf[it] }.toSet() }
    }

    @Volatile
    private var cachedIndex: ClusterIndex? = null

    private fun indexFor(clusters: List<TopicCluster>): ClusterIndex {
        val cached = cachedIndex
        if (cached != null && cached.clusters === clusters) return cached
        return ClusterIndex(clusters).also { idx ->
            cachedIndex = idx
            lessonContext?.setMacroMap(
                clusters.indices.mapNotNull { i -> idx.macroOf[i]?.let { clusters[i].topicId to it } }.toMap()
            )
        }
    }

    /** Resultado de recorrer el diccionario para un texto: depende solo del texto y del diccionario. */
    private class ScanResult(
        val bestTerm: AcademicTerm?,
        val bestSimilarity: Double,
        /** Hasta [MAX_CANDIDATES] términos con D >= [CANDIDATE_MIN_SIMILARITY], de mayor a menor D. */
        val top: List<Pair<AcademicTerm, Double>>
    )

    /** Caché acotada (LRU). Sincronizada: es barata frente a recorrer el diccionario. */
    private class BoundedCache<K, V>(private val capacity: Int) {
        private val map = object : LinkedHashMap<K, V>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > capacity
        }
        @Synchronized fun getOrPut(key: K, compute: () -> V): V = map[key] ?: compute().also { map[key] = it }
        @Synchronized fun clear() = map.clear()
    }

    /**
     * Memoria de recorridos del diccionario (latencia). Vosk reenvía la oración COMPLETA en cada parcial y otra vez
     * en la final, y en clase se repiten las mismas palabras: sin esto, cada una se comparaba con los ~1000 términos
     * una y otra vez. Es exacta (mismo texto + mismo diccionario = mismo resultado) y se vacía si cambia el diccionario.
     */
    private val wordScans = BoundedCache<String, ScanResult>(SCAN_CACHE_SIZE)
    private val spanScans = BoundedCache<String, ScanResult>(SCAN_CACHE_SIZE)
    @Volatile
    private var scannedDictionary: List<AcademicTerm>? = null

    /** Formas preparadas de cada término: él mismo y su plural (null si ya termina en "s"). */
    private class TermForms(val base: LevenshteinMatcher.Prepared, val plural: LevenshteinMatcher.Prepared?)
    private val termForms = java.util.concurrent.ConcurrentHashMap<String, TermForms>()

    private fun formsOf(term: String): TermForms = termForms.getOrPut(term) {
        TermForms(
            levenshteinMatcher.prepareTerm(term),
            if (term.lowercase(Locale.ROOT).endsWith("s")) null else levenshteinMatcher.prepareTerm(pluralOf(term))
        )
    }

    /** Términos sin tildes y en minúsculas, con su plural: uniones de palabras vacías que forman uno exacto. */
    @Volatile
    private var exactKeys: Pair<List<AcademicTerm>, Set<String>>? = null

    private fun exactTermKeys(dictionary: List<AcademicTerm>): Set<String> {
        exactKeys?.let { if (it.first === dictionary) return it.second }
        val keys = HashSet<String>()
        for (t in dictionary) {
            keys.add(stripDiacritics(t.term.lowercase(Locale.ROOT)))
            keys.add(stripDiacritics(pluralOf(t.term).lowercase(Locale.ROOT)))
        }
        exactKeys = Pair(dictionary, keys)
        return keys
    }

    /** Otro término de la oración comparte cluster con [term] (evidencia de la ORACIÓN, no solo de la clase). */
    private fun hasSentenceSupport(term: String, matchedTermsInSentence: Set<String>, clusters: List<TopicCluster>): Boolean {
        if (clusters.isEmpty()) return false
        val idx = indexFor(clusters)
        val lower = term.lowercase(Locale.ROOT)
        val mine = idx.byTerm[lower].orEmpty()
        return matchedTermsInSentence.any { other -> other != lower && idx.byTerm[other].orEmpty().any { it in mine } }
    }

    /** Reinicia la memoria de la clase (cambio de asignatura o nueva sesión). */
    fun resetLesson() {
        lessonContext?.reset()
    }


    /**
     * Umbral nítido de decisión especificado en la tesis (y* >= 0.65).
     */
    val decisionThreshold: Double = 0.65

    /**
     * Similitud mínima para que una palabra de la oración cuente como "término de contexto" en el
     * cálculo de S. Es deliberadamente más estricta que el umbral de candidato (0.50): con un
     * diccionario de ~400 términos casi cualquier palabra común supera 0.50 contra algún término
     * ("realiza"->"adrenalina"), lo que diluía S y bloqueaba correcciones legítimas.
     */
    val contextMatchThreshold: Double = 0.80

    private companion object {
        /** Similitud mínima para ser candidato a corrección (umbral de la tesis). */
        const val CANDIDATE_MIN_SIMILARITY = 0.50
        /** Máximo de candidatos que se re-evalúan por palabra (acota el costo a ≤ 3 inferencias). */
        const val MAX_CANDIDATES = 3
        /**
         * D mínima para SUSTITUIR, decida lo que decida el motor difuso. Con un tema vigente, S alto puede
         * llevar y* sobre el umbral aun con parecido pobre ("produce"->"profase", D=0.63): es un falso
         * positivo. La corrección legítima más débil observada es "sedula"->"célula" (D=0.73).
         */
        const val SUBSTITUTION_MIN_SIMILARITY = 0.70
        /** D que se considera "es el mismo término" (igual salvo tildes y mayúsculas). */
        const val EXACT_MATCH_SIMILARITY = 0.999
        /** D mínima de un candidato alternativo (no el mejor por D) para entrar al rerank. */
        const val RERANK_MIN_SIMILARITY = 0.70
        /** Solo compiten candidatos a menos de esta distancia de D respecto al mejor. */
        const val CANDIDATE_WINDOW = 0.12
        /**
         * Crédito parcial cuando el otro término está en OTRO cluster de la misma área temática
         * (p. ej. "célula" y "mitocondria": citología vs. organelos, ambos "biología celular").
         * Los clusters son finos y una oración real mezcla subtemas del mismo bloque.
         */
        const val MACRO_CREDIT = 0.5
        /** Peso máximo de la memoria de la clase frente a la evidencia de la propia oración. */
        const val LESSON_WEIGHT = 0.40
        /** Uniones de hasta 3 palabras ("de hizo soma" -> lisosoma). */
        const val SPAN_MAX_WORDS = 3
        /** Largo mínimo de la unión sin tildes: "y una" -> "yuna" no se une. */
        const val SPAN_MIN_CHARS = 6
        /** La unión debe parecerse claramente a un término (= contextMatchThreshold). */
        const val SPAN_MIN_SIMILARITY = 0.80
        /** ...y explicar mejor que cualquiera de sus pedazos por separado. */
        const val SPAN_MIN_GAIN = 0.05
        /**
         * C neutra (= AndroidSpeechEngine.NEUTRAL_CONFIDENCE) para UNIONES cuyo término el ASR no puede escribir:
         * si lo partió en pedazos es porque no lo conocía, y la confianza en esos pedazos no es evidencia en contra
         * del término. No se aplica a palabras enteras: "saber" escrita con seguridad sí es evidencia en contra de
         * "suber" (medido: aplicarlo a palabras sueltas produjo 6 falsas correcciones en el corpus de control SLR73).
         */
        const val OOV_NEUTRAL_CONFIDENCE = 0.65
        /**
         * Prefijos que Vosk deja sueltos delante de un término que sí escribe ("in saturado", "sin saturados",
         * "oxi acetilénica", "per óxido"). "de" NO está: "de óxido" -> dióxido unía una preposición real.
         */
        val PREFIX_PIECES = setOf("in", "sin", "des", "sub", "di", "tri", "per", "bi", "oxi", "ex")
        /** Textos distintos (palabras / uniones) que se recuerdan; una clase usa unos pocos miles. */
        const val SCAN_CACHE_SIZE = 8192
        /** Expresiones regulares compiladas una sola vez (compilarlas por palabra es caro en Android). */
        val EDGE_PUNCTUATION = Regex("^[^a-zA-Z0-9áéíóúüñÁÉÍÓÚÜÑ]+|[^a-zA-Z0-9áéíóúüñÁÉÍÓÚÜÑ]+$")
        val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
    }

    private data class PreScanMatch(
        val index: Int,
        val originalWord: String,
        val cleanWord: String,
        val trailingPunct: String,
        val confidence: Double,
        val bestTerm: AcademicTerm?,
        val bestSimilarity: Double,
        /** Candidatos cercanos al mejor (incluye al mejor), ordenados por D descendente. */
        val alternatives: List<Pair<AcademicTerm, Double>> = emptyList()
    )

    /**
     * Procesa una secuencia de palabras transcritas contra el diccionario en memoria
     * y los clusters temáticos de la materia activa mediante arquitectura de dos pasadas.
     */
    fun processSentence(
        rawWords: List<String>,
        confidences: List<Float>,
        dictionary: List<AcademicTerm>,
        clusters: List<TopicCluster> = emptyList(),
        /** true solo para oraciones FINALES del ASR: actualiza la memoria de la clase. Los parciales solo la consultan. */
        updateLessonMemory: Boolean = false,
        /**
         * Términos (en minúsculas) que el ASR NO puede escribir porque no están en su vocabulario (Vosk:
         * assets/dictionaries/biologia_vosk_oov.json). Vacío = se desconoce (motor de Android).
         */
        asrOutOfVocabulary: Set<String> = emptySet()
    ): List<WordCorrectionResult> {
        val preScans = mutableListOf<PreScanMatch>()
        if (scannedDictionary !== dictionary) {
            wordScans.clear()
            spanScans.clear()
            scannedDictionary = dictionary
        }

        // -------------------------------------------------------------
        // PASADA 1: Pre-escaneo léxico de la oración completa
        // -------------------------------------------------------------
        for (i in rawWords.indices) {
            val originalWord = rawWords[i]
            val confidence = (confidences.getOrNull(i) ?: 0.55f).toDouble()
            val cleanWord = originalWord.trim().replace(EDGE_PUNCTUATION, "")
            val trailingPunct = originalWord.trim().takeLastWhile { !it.isLetterOrDigit() }

            val wordToEvaluate = cleanWord.ifEmpty { originalWord }

            if (!posTagger.isCandidateNoun(wordToEvaluate) || dictionary.isEmpty()) {
                preScans.add(
                    PreScanMatch(
                        index = i,
                        originalWord = originalWord,
                        cleanWord = cleanWord,
                        trailingPunct = trailingPunct,
                        confidence = confidence,
                        bestTerm = null,
                        bestSimilarity = 0.0
                    )
                )
                continue
            }

            val scan = wordScans.getOrPut(wordToEvaluate) { scanWord(wordToEvaluate, dictionary) }
            val bestTerm = scan.bestTerm
            val bestSimilarity = scan.bestSimilarity
            val top = scan.top
            // El mejor por D siempre compite; los demás solo si están cerca Y son coincidencias decentes por sí
            // mismas. Sin este piso, un término del tema vigente con D≈0.6 le "robaba" palabras comunes
            // ("dentro"->"centriolo") solo por tener S alto.
            val alternatives = top.filterIndexed { idx, c ->
                idx == 0 || (c.second >= bestSimilarity - CANDIDATE_WINDOW && c.second >= RERANK_MIN_SIMILARITY)
            }

            preScans.add(
                PreScanMatch(
                    index = i,
                    originalWord = originalWord,
                    cleanWord = cleanWord,
                    trailingPunct = trailingPunct,
                    confidence = confidence,
                    bestTerm = bestTerm,
                    bestSimilarity = bestSimilarity,
                    alternatives = alternatives
                )
            )
        }

        // -------------------------------------------------------------
        // PASADA 1b: términos partidos por el ASR ("sito plasma" -> citoplasma)
        // -------------------------------------------------------------
        val splitTerms = if (splitTermMode != SplitTermMode.OFF && dictionary.isNotEmpty()) {
            findSplitTerms(preScans, dictionary)
        } else emptyMap()
        val coveredBySplit = splitTerms.values.flatMap { it.start until it.start + it.size }.toSet()

        // Términos de contexto: coincidencias sólidas con el vocabulario escolar (D >= contextMatchThreshold).
        // Una unión REEMPLAZA a sus pedazos como evidencia del tema: "plasma" (de "sito plasma") no debe votar en
        // contra de "citoplasma", y el término unido cuenta como una palabra con D >= 0.80.
        val matchedTermsInSentence = (preScans
            .filter { it.index !in coveredBySplit && it.bestTerm != null && it.bestSimilarity >= contextMatchThreshold }
            .mapNotNull { it.bestTerm?.term?.lowercase(Locale.ROOT) } +
            splitTerms.values
                .filter { it.similarity >= contextMatchThreshold }
                .map { it.alternatives.first().first.term.lowercase(Locale.ROOT) }).toSet()

        // -------------------------------------------------------------
        // PASADA 2: Evaluación Difusa Mamdani Tri-Variable (C, D, S)
        // -------------------------------------------------------------
        val results = mutableListOf<WordCorrectionResult>()
        // Términos "resueltos": sustituidos o ya bien escritos. Solo estos alimentan la memoria de la clase;
        // un candidato rechazado ("sedula"->médula descartado) no debe reforzar un tema equivocado.
        val resolvedTerms = mutableSetOf<String>()

        var k = 0
        while (k < preScans.size) {
            val split = splitTerms[k]
            if (split != null) {
                val merged = evaluateSplitTerm(split, preScans, matchedTermsInSentence, clusters, asrOutOfVocabulary)
                if (merged != null) {
                    results.add(merged.first)
                    resolvedTerms.add(merged.second)
                    k += split.size
                    continue
                }
                // Unión rechazada por el motor difuso: sus palabras se evalúan una por una, como siempre
            }
            evaluateSingleWord(preScans[k], matchedTermsInSentence, clusters, results, resolvedTerms)
            k++
        }


        if (updateLessonMemory && lessonContext != null && clusters.isNotEmpty()) {
            val idx = indexFor(clusters)
            lessonContext.recordTerms(
                resolvedTerms.map { term ->
                    idx.byTerm[term].orEmpty().map { clusters[it].topicId }
                }
            )
        }

        return results
    }

    /** Unión candidata de palabras seguidas que el ASR partió. */
    private data class SplitTerm(
        val start: Int,
        val size: Int,
        val joined: String,
        val similarity: Double,
        /** Candidatos para el rerank (el primero es el de mayor D), igual que con una palabra. */
        val alternatives: List<Pair<AcademicTerm, Double>>,
        /** C de la unión = la del pedazo menos confiable. */
        val confidence: Double,
        /** Algún pedazo ya era un término bien escrito ("plasma" en "sito plasma"). */
        val hasExactPiece: Boolean,
        /** Todos los pedazos son palabras vacías ("al que no"): exige otro término del mismo tema en la oración. */
        val functionWordsOnly: Boolean = false
    )

    /**
     * Plural regular. Una aguda con tilde en la última sílaba la pierde al volverse llana:
     * infección -> infecciones, catión -> cationes (antes salía "infecciónes").
     */
    internal fun pluralOf(term: String): String {
        if (stripDiacritics(term.takeLast(1).lowercase(Locale.ROOT)) in setOf("a", "e", "i", "o", "u")) return term + "s"
        val singular = if (term.length >= 2 && term[term.length - 1].lowercaseChar() in "ns" &&
            term[term.length - 2].lowercaseChar() in "áéíóú"
        ) term.dropLast(2) + stripDiacritics(term[term.length - 2].toString()) + term.last() else term
        return singular + "es"
    }

    /**
     * Forma que se muestra: el plural del término si lo dicho termina en "s" y se parece más al plural
     * ("arcanos" -> alcanos, "game tos" -> gametos). Solo cambia la forma escrita, no la decisión.
     */
    private fun surfaceForm(spoken: String, term: String): String {
        if (!spoken.lowercase(Locale.ROOT).endsWith("s") || term.lowercase(Locale.ROOT).endsWith("s")) return term
        val prepared = levenshteinMatcher.prepare(spoken)
        val plural = pluralOf(term)
        return if (levenshteinMatcher.combinedSimilarity(prepared, levenshteinMatcher.prepareTerm(plural)) >
            levenshteinMatcher.combinedSimilarity(prepared, levenshteinMatcher.prepareTerm(term))
        ) plural else term
    }

    /** D de una unión contra un término, admitiendo su plural ("game tos" -> gametos). */
    private fun spanSimilarity(joined: LevenshteinMatcher.Prepared, joinedEndsWithS: Boolean, forms: TermForms): Double {
        val sim = levenshteinMatcher.combinedSimilarity(joined, forms.base)
        val plural = forms.plural
        if (!joinedEndsWithS || plural == null) return sim
        return maxOf(sim, levenshteinMatcher.combinedSimilarity(joined, plural))
    }

    /** Inserta en el top-K ordenado (K pequeño: sin ordenar todo el diccionario). Empates: queda el primero. */
    private fun offerCandidate(top: ArrayList<Pair<AcademicTerm, Double>>, term: AcademicTerm, sim: Double) {
        if (sim >= CANDIDATE_MIN_SIMILARITY && (top.size < MAX_CANDIDATES || sim > top.last().second)) {
            var pos = top.size
            while (pos > 0 && top[pos - 1].second < sim) pos--
            top.add(pos, Pair(term, sim))
            if (top.size > MAX_CANDIDATES) top.removeAt(top.size - 1)
        }
    }

    /** Recorre el diccionario completo para una palabra (la palabra se normaliza una sola vez). */
    private fun scanWord(word: String, dictionary: List<AcademicTerm>): ScanResult {
        var bestTerm: AcademicTerm? = null
        var bestSimilarity = 0.0
        val top = ArrayList<Pair<AcademicTerm, Double>>(MAX_CANDIDATES + 1)
        val prepared = levenshteinMatcher.prepare(word)
        for (term in dictionary) {
            val sim = levenshteinMatcher.combinedSimilarity(prepared, formsOf(term.term).base)
            if (sim > bestSimilarity) {
                bestSimilarity = sim
                bestTerm = term
            }
            offerCandidate(top, term, sim)
        }
        return ScanResult(bestTerm, bestSimilarity, top)
    }

    /**
     * Recorre el diccionario para una unión. Una unión solo se acepta con D >= [SPAN_MIN_SIMILARITY] y sus
     * alternativas necesitan D >= [RERANK_MIN_SIMILARITY]: un término cuya cota de largo
     * ([LevenshteinMatcher.combinedSimilarityUpperBound]) no llega a [RERANK_MIN_SIMILARITY] no puede cambiar el
     * resultado y se salta sin calcular Levenshtein (era ~85 % del costo de la capa con frases largas).
     */
    private fun scanSpan(joined: String, dictionary: List<AcademicTerm>): ScanResult {
        val prepared = levenshteinMatcher.prepare(joined)
        val endsWithS = joined.lowercase(Locale.ROOT).endsWith("s")
        var bestTerm: AcademicTerm? = null
        var bestSimilarity = 0.0
        val top = ArrayList<Pair<AcademicTerm, Double>>(MAX_CANDIDATES + 1)
        for (term in dictionary) {
            val forms = formsOf(term.term)
            val plural = forms.plural.takeIf { endsWithS }
            if (levenshteinMatcher.combinedSimilarityUpperBound(prepared, forms.base) < RERANK_MIN_SIMILARITY &&
                (plural == null || levenshteinMatcher.combinedSimilarityUpperBound(prepared, plural) < RERANK_MIN_SIMILARITY)
            ) continue
            val sim = spanSimilarity(prepared, endsWithS, forms)
            if (sim > bestSimilarity) {
                bestSimilarity = sim
                bestTerm = term
            }
            offerCandidate(top, term, sim)
        }
        return ScanResult(bestTerm, bestSimilarity, top)
    }

    /** "plasma" es parte propia de "citoplasma"; "fotosíntesis" no lo es de sí mismo ("la fotosintesis"). */
    private fun isProperPart(pieceTerm: String, term: String): Boolean {
        val p = stripDiacritics(pieceTerm.lowercase(Locale.ROOT))
        val t = stripDiacritics(term.lowercase(Locale.ROOT))
        return p.length < t.length && t.contains(p)
    }

    /**
     * Vosk solo escribe palabras de su vocabulario: un término que no conoce sale en pedazos ("citoplasma" ->
     * "sito plasma", "procariota" -> "pero carlota"). Se prueban uniones de 2 a [SPAN_MAX_WORDS] palabras seguidas
     * que se parezcan a un término (D >= [SPAN_MIN_SIMILARITY]) claramente más que cualquiera de sus pedazos.
     * Se eligen las de mayor D sin solaparse. Espejo de find_split_terms (evaluation/benchmark_runner.py).
     */
    private fun findSplitTerms(preScans: List<PreScanMatch>, dictionary: List<AcademicTerm>): Map<Int, SplitTerm> {
        val found = mutableListOf<SplitTerm>()
        for (i in preScans.indices) {
            for (size in 2..SPAN_MAX_WORDS) {
                if (i + size > preScans.size) break
                val items = preScans.subList(i, i + size)
                val words = items.map { it.cleanWord.ifEmpty { it.originalWord } }
                val joined = words.joinToString("")
                if (stripDiacritics(joined).length < SPAN_MIN_CHARS) continue
                // Solo palabras vacías ("al que no" -> alqueno): se une únicamente si forman el término EXACTO (o su
                // plural); además, evaluateSplitTerm exige otro término del mismo tema en la oración.
                val functionWordsOnly = words.none { posTagger.isCandidateNoun(it) }
                if (functionWordsOnly && stripDiacritics(joined.lowercase(Locale.ROOT)) !in exactTermKeys(dictionary)) continue

                val exact = items.indices.filter { j ->
                    val it = items[j]
                    val term = it.bestTerm
                    term != null && (it.bestSimilarity >= EXACT_MATCH_SIMILARITY ||
                        isInflectionOf(it.cleanWord.ifEmpty { it.originalWord }, term.term))
                }
                if (exact.isNotEmpty() && splitTermMode == SplitTermMode.STRICT) continue
                // Con un pedazo ya exacto, la unión no puede empezar ni terminar en una palabra vacía: pegada al
                // término desde afuera formaba otro término ("de óxido" -> dióxido, "la polar" -> apolar,
                // "de nitrificación" -> desnitrificación, "metano el" -> metanol). En medio sí se admite: es el
                // pegamento de los pedazos de Vosk ("sean o bacterias" -> cianobacterias).
                // Excepción: un prefijo que el ASR separó ("in saturado" -> insaturado, "oxi acetilénica").
                if (exact.isNotEmpty() && listOf(0, items.size - 1).any {
                        it !in exact && !posTagger.isCandidateNoun(words[it]) &&
                            stripDiacritics(words[it].lowercase(Locale.ROOT)) !in PREFIX_PIECES
                    }) continue

                // Mejor término y candidatos cercanos (misma regla de rerank que una palabra)
                val scan = spanScans.getOrPut(joined) { scanSpan(joined, dictionary) }
                val bestTerm = scan.bestTerm
                val bestSimilarity = scan.bestSimilarity
                val top = scan.top
                val best = bestTerm ?: continue
                if (bestSimilarity < SPAN_MIN_SIMILARITY) continue
                if (exact.isNotEmpty() && !exact.all { isProperPart(items[it].bestTerm!!.term, best.term) }) continue
                val strongestPiece = items.indices.filter { it !in exact }.maxOfOrNull { items[it].bestSimilarity } ?: 0.0
                if (bestSimilarity < strongestPiece + SPAN_MIN_GAIN) continue

                found.add(
                    SplitTerm(
                        start = i,
                        size = size,
                        joined = joined,
                        similarity = bestSimilarity,
                        alternatives = top.filterIndexed { idx, c ->
                            idx == 0 || (c.second >= bestSimilarity - CANDIDATE_WINDOW && c.second >= RERANK_MIN_SIMILARITY)
                        },
                        confidence = items.minOf { it.confidence },
                        hasExactPiece = exact.isNotEmpty(),
                        functionWordsOnly = functionWordsOnly
                    )
                )
            }
        }
        val chosen = HashMap<Int, SplitTerm>()
        val used = HashSet<Int>()
        for (split in found.sortedWith(compareBy<SplitTerm>({ -it.similarity }, { -it.size }, { it.start }))) {
            val covered = split.start until split.start + split.size
            if (covered.any { it in used }) continue
            chosen[split.start] = split
            used.addAll(covered)
        }
        return chosen
    }

    /**
     * Decide con (C, D, S) si una unión se sustituye por su término. null = rechazada (las palabras se evalúan
     * sueltas). Devuelve el resultado y el término resuelto para la memoria de la clase.
     */
    private fun evaluateSplitTerm(
        split: SplitTerm,
        preScans: List<PreScanMatch>,
        matchedTermsInSentence: Set<String>,
        clusters: List<TopicCluster>,
        asrOutOfVocabulary: Set<String>
    ): Pair<WordCorrectionResult, String>? {
        val candidates = (if (rerankCandidates) split.alternatives else split.alternatives.take(1))
            .filter { !split.functionWordsOnly || hasSentenceSupport(it.first.term, matchedTermsInSentence, clusters) }
        if (candidates.isEmpty()) return null
        var bestTerm = candidates.first().first
        var bestSimilarity = candidates.first().second
        var fuzzyScore = -1.0
        var semanticCoherence = 0.0
        var activeCluster: String? = null
        var usedConfidence = split.confidence
        for ((candidate, sim) in candidates) {
            val (s, cluster) = calculateSemanticCoherence(candidate.term, matchedTermsInSentence, clusters)
            // Con un pedazo ya bien escrito el ASR sí escribió con seguridad una palabra real: se usa su C tal cual
            val c = if (!split.hasExactPiece && candidate.term.lowercase(Locale.ROOT) in asrOutOfVocabulary) {
                minOf(split.confidence, OOV_NEUTRAL_CONFIDENCE)
            } else split.confidence
            val y = fuzzyEngine.evaluate(confianza = c, similitud = sim, semantica = s)
            if (y > fuzzyScore + 1e-9 || (kotlin.math.abs(y - fuzzyScore) <= 1e-9 && sim > bestSimilarity)) {
                fuzzyScore = y
                bestTerm = candidate
                bestSimilarity = sim
                semanticCoherence = s
                activeCluster = cluster
                usedConfidence = c
            }
        }
        if (fuzzyScore < decisionThreshold || bestSimilarity < SUBSTITUTION_MIN_SIMILARITY) return null

        val items = preScans.subList(split.start, split.start + split.size)
        val surface = surfaceForm(split.joined, bestTerm.term)
        val first = items.first()
        return Pair(
            WordCorrectionResult(
                originalWord = items.joinToString(" ") { it.originalWord },
                finalWord = matchCase(first.cleanWord.ifEmpty { first.originalWord }, surface) + items.last().trailingPunct,
                acousticConfidence = usedConfidence,
                lexicalSimilarity = bestSimilarity,
                fuzzyJustification = fuzzyScore,
                wasSubstituted = true,
                semanticCoherence = semanticCoherence,
                topicCluster = activeCluster
            ),
            bestTerm.term.lowercase(Locale.ROOT)
        )
    }

    /** Evalúa UNA palabra con (C, D, S) y, si y* lo justifica, la sustituye por el término del diccionario. */
    private fun evaluateSingleWord(
        item: PreScanMatch,
        matchedTermsInSentence: Set<String>,
        clusters: List<TopicCluster>,
        results: MutableList<WordCorrectionResult>,
        resolvedTerms: MutableSet<String>
    ) {
        val firstTerm = item.bestTerm
        val firstSimilarity = item.bestSimilarity

        if (firstTerm == null || firstSimilarity < CANDIDATE_MIN_SIMILARITY) {
            results.add(
                WordCorrectionResult(
                    originalWord = item.originalWord,
                    finalWord = item.originalWord,
                    acousticConfidence = item.confidence,
                    lexicalSimilarity = firstSimilarity,
                    fuzzyJustification = 0.0,
                    wasSubstituted = false,
                    semanticCoherence = 0.0,
                    topicCluster = null
                )
            )
            return
        }

        // Selección del candidato: con rerank se evalúan los candidatos cercanos con (C, D, S) y gana el
        // de mayor y*; empate -> mayor D. Sin rerank (o un único candidato) gana el de mayor D.
        val candidates = if (rerankCandidates && item.alternatives.size > 1) item.alternatives
        else listOf(Pair(firstTerm, firstSimilarity))

        var bestTerm: AcademicTerm = firstTerm
        var bestSimilarity = firstSimilarity
        var semanticCoherence = 0.0
        var activeCluster: String? = null
        var fuzzyScore = -1.0
        for ((candidate, sim) in candidates) {
            // Factor Semántico S del candidato (co-ocurrencia en la oración + memoria de la clase)
            val (s, cluster) = calculateSemanticCoherence(
                currentCandidateTerm = candidate.term,
                allMatchedTermsInSentence = matchedTermsInSentence,
                clusters = clusters
            )
            // Inferencia Mamdani tri-variable de 27 reglas
            val y = fuzzyEngine.evaluate(confianza = item.confidence, similitud = sim, semantica = s)
            if (y > fuzzyScore + 1e-9 || (kotlin.math.abs(y - fuzzyScore) <= 1e-9 && sim > bestSimilarity)) {
                fuzzyScore = y
                bestTerm = candidate
                bestSimilarity = sim
                semanticCoherence = s
                activeCluster = cluster
            }
        }

        // Ya correcta = idéntica al término (salvo mayúsculas) o su plural/singular. Sustituirla no cambia
        // nada visible y solo inflaba el contador de correcciones y el historial ("núcleo -> núcleo").
        // Una diferencia de tildes ("celula" -> "célula") SÍ cuenta como corrección.
        val spokenWord = item.cleanWord.ifEmpty { item.originalWord }
        val alreadyCorrect = spokenWord.equals(bestTerm.term, ignoreCase = true) ||
            isInflectionOf(spokenWord, bestTerm.term)
        if (fuzzyScore >= decisionThreshold &&
            bestSimilarity >= SUBSTITUTION_MIN_SIMILARITY &&
            !alreadyCorrect
        ) {
            // Sustitución silenciosa aprobada por el umbral y* >= 0.65
            val wordBase = item.cleanWord.ifEmpty { item.originalWord }
            val preservedCaseWord = matchCase(wordBase, surfaceForm(wordBase, bestTerm.term)) + item.trailingPunct
            resolvedTerms.add(bestTerm.term.lowercase(Locale.ROOT))

            results.add(
                WordCorrectionResult(
                    originalWord = item.originalWord,
                    finalWord = preservedCaseWord,
                    acousticConfidence = item.confidence,
                    lexicalSimilarity = bestSimilarity,
                    fuzzyJustification = fuzzyScore,
                    wasSubstituted = true,
                    semanticCoherence = semanticCoherence,
                    topicCluster = activeCluster
                )
            )
        } else {
            // No superó el umbral: se conserva la hipótesis original del ASR. Si la palabra YA es el
            // término (igual salvo tildes/mayúsculas, o su plural/singular) cuenta como evidencia de tema.
            if (bestSimilarity >= EXACT_MATCH_SIMILARITY || alreadyCorrect) {
                resolvedTerms.add(bestTerm.term.lowercase(Locale.ROOT))
            }
            results.add(
                WordCorrectionResult(
                    originalWord = item.originalWord,
                    finalWord = item.originalWord,
                    acousticConfidence = item.confidence,
                    lexicalSimilarity = bestSimilarity,
                    fuzzyJustification = fuzzyScore,
                    wasSubstituted = false,
                    semanticCoherence = semanticCoherence,
                    topicCluster = activeCluster
                )
            )
        }
    }

    /**
     * Calcula la coherencia semántica S en [0.0, 1.0] evaluando cuántos otros términos
     * académicos de la oración co-ocurren en los mismos clusters temáticos del término candidato.
     *
     * @return Par de (puntuación S, nombre del cluster con mayor afinidad)
     */
    fun calculateSemanticCoherence(
        currentCandidateTerm: String,
        allMatchedTermsInSentence: Set<String>,
        clusters: List<TopicCluster>
    ): Pair<Double, String?> {
        if (clusters.isEmpty()) {
            return Pair(0.50, null) // Si no hay clusters configurados, mantener factor neutro
        }

        val idx = indexFor(clusters)
        val candidateLower = currentCandidateTerm.lowercase(Locale.ROOT)
        val matching = idx.byTerm[candidateLower].orEmpty()

        if (matching.isEmpty()) {
            return Pair(0.20, null) // Término no indexado en clusters temáticos
        }

        // (a) Evidencia de la oración: fracción de otros términos sólidos que co-ocurren en un mismo cluster
        var sentenceScore: Double? = null
        var bestClusterName: String? = clusters[matching.first()].topicName
        val otherTerms = allMatchedTermsInSentence.filter { it != candidateLower }
        if (otherTerms.isNotEmpty()) {
            var bestScore = 0.0
            for (ci in matching) {
                val set = idx.termSets[ci]
                val macro = idx.macroOf[ci]
                var credit = 0.0
                for (other in otherTerms) {
                    credit += when {
                        other in set -> 1.0 // mismo cluster
                        macro != null && macro in idx.termMacros[other].orEmpty() -> MACRO_CREDIT // misma área temática
                        else -> 0.0
                    }
                }
                val score = credit / otherTerms.size.toDouble()
                if (score >= bestScore) {
                    bestScore = score
                    bestClusterName = clusters[ci].topicName
                }
            }
            sentenceScore = bestScore.coerceIn(0.0, 1.0)
        }

        // (b) Evidencia de la clase: afinidad del candidato con el tema vigente en la memoria
        val lesson = lessonContext?.affinity(matching.map { clusters[it].topicId })
        if (lesson != null && sentenceScore == null) {
            // Sin contexto en la oración, el nombre del cluster se toma de la memoria de la clase
            clusters.firstOrNull { it.topicId == lesson.bestClusterId }?.let { bestClusterName = it.topicName }
        }

        return Pair(combineSemanticEvidence(sentenceScore, lesson), bestClusterName)
    }

    /**
     * Combina la evidencia de la oración con la memoria de la clase en un único S in [0.0, 1.0].
     * - Sin ninguna evidencia: neutro 0.50.
     * - Solo clase: se acerca a la afinidad de la clase en proporción a la fuerza de la memoria.
     * - Solo oración: se conserva el cálculo original de la tesis.
     * - Ambas: mezcla ponderada; la clase pesa hasta [LESSON_WEIGHT] según su fuerza.
     */
    internal fun combineSemanticEvidence(sentenceScore: Double?, lesson: LessonAffinity?): Double {
        return when {
            sentenceScore == null && lesson == null -> 0.50
            sentenceScore == null -> 0.50 + (lesson!!.value - 0.50) * lesson.strength
            lesson == null -> sentenceScore
            else -> {
                val w = LESSON_WEIGHT * lesson.strength
                (1.0 - w) * sentenceScore + w * lesson.value
            }
        }.coerceIn(0.0, 1.0)
    }

    /**
     * true si [word] es el plural/singular regular de [term] ("gametos"/"gameto"): ya está bien escrita y
     * sustituirla cambiaría el número gramatical que el estudiante lee. Comparación sin tildes ni mayúsculas.
     */
    internal fun isInflectionOf(word: String, term: String): Boolean {
        val w = stripDiacritics(word.lowercase(Locale.ROOT))
        val t = stripDiacritics(term.lowercase(Locale.ROOT))
        if (w == t) return false
        return w == t + "s" || w == t + "es" || t == w + "s" || t == w + "es"
    }

    private fun stripDiacritics(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(DIACRITICS, "")

    private fun matchCase(source: String, target: String): String {
        return if (source.firstOrNull { it.isLetter() }?.isUpperCase() == true) {
            target.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            target
        }
    }
}
