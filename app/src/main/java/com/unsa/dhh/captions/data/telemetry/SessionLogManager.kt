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
    val averageFuzzyScore: Double
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
                latencyMs = latencyMs
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
     * Calcula las estadísticas agregadas de la sesión actual.
     */
    fun getSessionStats(): SessionStats {
        synchronized(inMemoryLogs) {
            if (inMemoryLogs.isEmpty()) {
                return SessionStats(0, 0, 0L, 0.0, 0.0)
            }

            val total = inMemoryLogs.size
            val subs = inMemoryLogs.count { it.substituted }
            val avgLatency = inMemoryLogs.map { it.latencyMs }.average().toLong()
            val avgConf = inMemoryLogs.map { it.acousticConfidence.toDouble() }.average()
            val avgFuzzy = inMemoryLogs.map { it.fuzzyJustification.toDouble() }.average()

            return SessionStats(
                totalWords = total,
                totalSubstitutions = subs,
                averageLatencyMs = avgLatency,
                averageConfidence = avgConf,
                averageFuzzyScore = avgFuzzy
            )
        }
    }

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
}
