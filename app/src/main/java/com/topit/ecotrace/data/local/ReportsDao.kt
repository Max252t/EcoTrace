package com.topit.ecotrace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportsDao {
    @Query("SELECT * FROM reports ORDER BY createdAtEpochSeconds DESC")
    fun observeReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<ReportEntity>)

    @Update
    suspend fun update(report: ReportEntity)

    @Query("UPDATE reports SET status = :status, synced = 0 WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM reports WHERE synced = 0")
    suspend fun getUnsyncedReports(): List<ReportEntity>

    @Query("UPDATE reports SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: String)
}
