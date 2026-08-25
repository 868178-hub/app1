package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSessionDao {
    @Query("SELECT * FROM run_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<RunSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RunSessionEntity)

    @Query("DELETE FROM run_sessions")
    suspend fun deleteAll()
}
