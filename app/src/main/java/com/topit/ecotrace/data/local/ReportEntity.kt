package com.topit.ecotrace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val imageUri: String?,
    val authorId: String,
    val createdAtEpochSeconds: Long,
    val synced: Boolean,
)
