package com.unsa.dhh.captions.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}
