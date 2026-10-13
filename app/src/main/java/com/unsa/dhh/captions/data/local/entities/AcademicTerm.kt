package com.unsa.dhh.captions.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad relacional para términos de vocabulario académico del MINEDU.
 */
@Entity(
    tableName = "academic_terms",
    indices = [
        Index(value = ["term"]),
        Index(value = ["subject"])
    ]
)
data class AcademicTerm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val term: String,
    val lemma: String,
    val subject: String, // "BIOLOGIA", "QUIMICA", "HISTORIA", "GEOGRAFIA"
    val category: String, // "NOUN", "PROPN"
    val phonetic: String
)
