package com.topit.ecotrace.data.local

import androidx.room.Entity

@Entity(tableName = "achievements", primaryKeys = ["userId", "code"])
data class AchievementEntity(
    val userId: String,
    val code: String,
    val unlockedAtEpochSeconds: Long,
    val synced: Boolean,
)
