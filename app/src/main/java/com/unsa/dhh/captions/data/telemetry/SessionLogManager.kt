package com.unsa.dhh.captions.data.telemetry

import com.unsa.dhh.captions.data.decision.WordCorrectionResult
import com.unsa.dhh.captions.data.local.dao.SessionLogDao
import com.unsa.dhh.captions.data.local.entities.SessionLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Estadísticas de rendimiento acústico y precisión de la sesión escolar.
 */
data class SessionStats(
    val totalWords: Int,
    val totalSubstitutions: Int,
    val averageLatencyMs: Long,
    val averageConfidence: Double,
    val averageFuzzyScore: Double,
    val averageSemanticScore: Double = 0.0,
    val keywordErrorRate: Double = 0.0,
    val keywordPrecision: Double = 1.0
)

/**
 * Módulo 4: Gestor de Telemetría de Sesión y Cálculo de Métricas de Error.
 * Almacena los registros para auditoría experimental y calcula objetivamente
 * el WER (Word Error Rate): WER = (S + D + I) / N.
 */
class SessionLogManager(
    private val sessionLogDao: SessionLogDao? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val inMemoryLogs = mutableListOf<SessionLogEntity>()

    /**
     * Limpia los registros acumulados en memoria para iniciar una nueva sesión limpia.
     */
    fun resetSession() {
        synchronized(inMemoryLogs) {
            inMemoryLogs.clear()
        }
    }

    /**
     * Registra el procesamiento de una frase con su latencia de inferencia y despliegue.
     */
    fun recordBatch(
        sessionId: String,
        subject: String,
        results: List<WordCorrectionResult>,
        latencyMs: Long
    ) {
        val now = System.currentTimeMillis()
        for (res in results) {
            val entity = SessionLogEntity(
                sessionId = sessionId,
                subject = subject,
                timestamp = now,
                originalWord = res.originalWord,
                correctedWord = res.finalWord,
                acousticConfidence = res.acousticConfidence.toFloat(),
                lexicalSimilarity = res.lexicalSimilarity.toFloat(),
                fuzzyJustification = res.fuzzyJustification.toFloat(),
                substituted = res.wasSubstituted,
                latencyMs = latencyMs,
                semanticCoherence = res.semanticCoherence.toFloat(),
                topicCluster = res.topicCluster
            )

            synchronized(inMemoryLogs) {
                inMemoryLogs.add(entity)
            }

            sessionLogDao?.let { dao ->
                scope.launch {
                    try {
                        dao.insertLog(entity)
                    } catch (e: Exception) {
                        // Tolerar fallo transitorio de inserción
                    }
                }
            }
        }
    }

    /**
     * Obtiene la lista completa de palabras sustituidas/corregidas durante la sesión.
     */
    fun getCorrectionsHistory(): List<SessionLogEntity> {
        synchronized(inMemoryLogs) {
            return inMemoryLogs
                .filter { it.substituted || !it.originalWord.equals(it.correctedWord, ignoreCase = true) }
                .sortedByDescending { it.timestamp }
        }
    }

    /**
     * Calcula las estadísticas agregadas de la sesión actual, incluyendo opcionalmente
     * el Keyword Error Rate (KER) y la Precisión Semántica si se proveen términos clave.
     */
    fun getSessionStats(keywordTerms: Set<String> = emptySet()): SessionStats {
        synchronized(inMemoryLogs) {
            if (inMemoryLogs.isEmpty()) {
                return SessionStats(0, 0, 0L, 0.0, 0.0, 0.0, 0.0, 1.0)
            }

            val total = inMemoryLogs.size
            val subs = inMemoryLogs.count { it.substituted }
            val avgLatency = inMemoryLogs.map { it.latencyMs }.average().toLong()
            val avgConf = inMemoryLogs.map { it.acousticConfidence.toDouble() }.average()
            val avgFuzzy = inMemoryLogs.map { it.fuzzyJustification.toDouble() }.average()
            // S media solo de los TÉRMINOS TÉCNICOS (corregidos o que coinciden con el diccionario, D >= 0.80).
            // Las palabras comunes ("la", "tiene") no tienen tema y valen S = 0: incluirlas diluía el promedio.
            val avgSemantic = inMemoryLogs
                .filter { it.substituted || it.lexicalSimilarity >= TECHNICAL_TERM_MIN_SIMILARITY }
                .map { it.semanticCoherence.toDouble() }
                .average().let { if (it.isNaN()) 0.0 else it }

            val (ker, precision) = if (keywordTerms.isNotEmpty()) {
                val hyp = inMemoryLogs.joinToString(" ") { it.correctedWord }
                val ref = inMemoryLogs.joinToString(" ") { if (it.substituted) it.correctedWord else it.originalWord }
                val k = calculateKer(hyp, ref, keywordTerms)
                Pair(k, (1.0 - k).coerceIn(0.0, 1.0))
            } else {
                Pair(0.0, 1.0)
            }

            return SessionStats(
                totalWords = total,
                totalSubstitutions = subs,
                averageLatencyMs = avgLatency,
                averageConfidence = avgConf,
                averageFuzzyScore = avgFuzzy,
                averageSemanticScore = avgSemantic,
                keywordErrorRate = ker,
                keywordPrecision = precision
            )
        }
    }

    /**
     * Normaliza un texto para comparación léxica uniforme (minúsculas, sin diacríticos ni puntuación superflua).
     */
    fun normalizeText(text: String): String {
        val nfkd = java.text.Normalizer.normalize(text.lowercase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(nfkd, "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Tasa de error de términos clave y precisión semántica:
     *   KER = (N_k - A_k) / N_k        Precisión semántica = A_k / N_k = 1 - KER
     *   N_k = apariciones de términos clave en la referencia
     *   A_k = cuántas de ellas aparecen también en la hipótesis (como máximo, tantas como en la referencia)
     * Se compara palabra por palabra tras normalizar (minúsculas, sin tildes ni puntuación);
     * admite términos de una o varias palabras.
     *
     * @return KER en [0.0, 1.0]; 0.0 si la referencia no contiene términos clave.
     */
    fun calculateKer(
        hypothesis: String,
        reference: String,
        keywordTerms: Set<String>
    ): Double {
        val referencia = normalizeText(reference).split(" ")
        val hipotesis = normalizeText(hypothesis).split(" ")
        var n = 0
        var a = 0
        for (termino in keywordTerms.map { normalizeText(it) }.filter { it.isNotBlank() }.toSet()) {
            val palabras = termino.split(" ")
            val enRef = contarTermino(palabras, referencia)
            if (enRef > 0) {
                n += enRef
                a += min(enRef, contarTermino(palabras, hipotesis))
            }
        }
        return if (n == 0) 0.0 else (n - a).toDouble() / n
    }

    /** Veces que la secuencia de palabras [termino] aparece en [palabras]. */
    private fun contarTermino(termino: List<String>, palabras: List<String>): Int =
        palabras.windowed(termino.size).count { it == termino }

    /**
     * Calcula la Tasa de Error de Palabra (WER) formal:
     * WER = (S + D + I) / N
     *
     * @param hypothesis Texto obtenido por el sistema
     * @param reference Texto de referencia humana (Ground Truth)
     * @return Tasa de error en rango [0.0, inf) (0.0 = 100% exactitud)
     */
    fun calculateWer(hypothesis: String, reference: String): Double {
        val hypWords = hypothesis.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val refWords = reference.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        if (refWords.isEmpty()) {
            return if (hypWords.isEmpty()) 0.0 else 1.0
        }

        val dTable = Array(refWords.size + 1) { IntArray(hypWords.size + 1) }

        for (i in 0..refWords.size) dTable[i][0] = i
        for (j in 0..hypWords.size) dTable[0][j] = j

        for (i in 1..refWords.size) {
            for (j in 1..hypWords.size) {
                if (refWords[i - 1] == hypWords[j - 1]) {
                    dTable[i][j] = dTable[i - 1][j - 1]
                } else {
                    val substitution = dTable[i - 1][j - 1] + 1
                    val deletion = dTable[i - 1][j] + 1
                    val insertion = dTable[i][j - 1] + 1
                    dTable[i][j] = min(substitution, min(deletion, insertion))
                }
            }
        }

        val editDistance = dTable[refWords.size][hypWords.size]
        return editDistance.toDouble() / refWords.size.toDouble()
    }

    private companion object {
        /** D mínima para que una palabra no corregida cuente como término técnico en la S media de la sesión. */
        const val TECHNICAL_TERM_MIN_SIMILARITY = 0.80f
    }
}
