package com.unsa.dhh.captions.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de telemetría y registro de sesión para validación algorítmica y cálculo de WER.
 */
@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val subject: String,
    val timestamp: Long,
    val originalWord: String,
    val correctedWord: String,
    val acousticConfidence: Float,
    val lexicalSimilarity: Float,
    val fuzzyJustification: Float,
    val substituted: Boolean,
    val latencyMs: Long
)
