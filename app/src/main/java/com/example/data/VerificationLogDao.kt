package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationLogDao {
    @Query("SELECT * FROM verification_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<VerificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VerificationLog)

    @Query("DELETE FROM verification_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM verification_logs")
    suspend fun clearAllLogs()
}
