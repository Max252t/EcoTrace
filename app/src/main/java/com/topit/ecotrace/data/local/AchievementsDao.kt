package com.topit.ecotrace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementsDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY unlockedAtEpochSeconds ASC")
    fun observeForUser(userId: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getForUser(userId: String): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND synced = 0")
    suspend fun getUnsynced(userId: String): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Query("DELETE FROM achievements")
    suspend fun clear()
}
