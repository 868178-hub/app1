package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TerritoryDao {
    @Query("SELECT * FROM territories ORDER BY capturedAt DESC")
    fun getAllTerritories(): Flow<List<TerritoryEntity>>

    @Query("SELECT * FROM territories WHERE isUserOwned = 1 ORDER BY capturedAt DESC")
    fun getUserTerritories(): Flow<List<TerritoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritory(territory: TerritoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(territories: List<TerritoryEntity>)

    @Query("DELETE FROM territories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM territories")
    suspend fun deleteAll()
}
