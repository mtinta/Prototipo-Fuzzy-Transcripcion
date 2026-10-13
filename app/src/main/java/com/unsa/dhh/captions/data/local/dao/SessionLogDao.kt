package com.unsa.dhh.captions.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unsa.dhh.captions.data.local.entities.SessionLogEntity

@Dao
interface SessionLogDao {

    @Insert
    suspend fun insertLog(log: SessionLogEntity)

    @Query("SELECT * FROM session_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLogsBySession(sessionId: String): List<SessionLogEntity>

    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC LIMIT 500")
    suspend fun getRecentLogs(): List<SessionLogEntity>

    @Query("DELETE FROM session_logs")
    suspend fun clearLogs()
}
