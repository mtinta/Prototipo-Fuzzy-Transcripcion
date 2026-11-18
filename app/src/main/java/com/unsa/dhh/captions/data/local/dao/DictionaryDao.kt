package com.unsa.dhh.captions.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.unsa.dhh.captions.data.local.entities.AcademicTerm

@Dao
interface DictionaryDao {

    @Query("SELECT * FROM academic_terms WHERE subject = :subject")
    suspend fun getTermsBySubject(subject: String): List<AcademicTerm>

    @Query("SELECT * FROM academic_terms")
    suspend fun getAllTerms(): List<AcademicTerm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terms: List<AcademicTerm>)

    @Query("SELECT COUNT(*) FROM academic_terms")
    suspend fun getTermCount(): Int

    @Query("DELETE FROM academic_terms WHERE subject = :subject")
    suspend fun deleteBySubject(subject: String)

    /** Reemplaza los términos de un área por los del APK (la base solo se llenaba al crearse y quedaba vieja). */
    @Transaction
    suspend fun replaceSubject(subject: String, terms: List<AcademicTerm>) {
        deleteBySubject(subject)
        insertAll(terms)
    }
}
