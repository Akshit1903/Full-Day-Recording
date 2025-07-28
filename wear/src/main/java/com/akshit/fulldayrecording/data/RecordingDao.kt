package com.akshit.fulldayrecording.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY startTime DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>
    
    @Query("SELECT * FROM recordings WHERE isSynced = 0 ORDER BY startTime ASC")
    fun getUnsyncedRecordings(): Flow<List<RecordingEntity>>
    
    @Insert
    suspend fun insertRecording(recording: RecordingEntity): Long
    
    @Update
    suspend fun updateRecording(recording: RecordingEntity)
    
    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)
    
    @Query("DELETE FROM recordings WHERE isSynced = 1")
    suspend fun deleteSyncedRecordings()
    
    @Query("SELECT COUNT(*) FROM recordings WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
} 