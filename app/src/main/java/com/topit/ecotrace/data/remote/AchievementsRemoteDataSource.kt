package com.topit.ecotrace.data.remote

import com.topit.ecotrace.domain.model.Achievement

interface AchievementsRemoteDataSource {
    suspend fun fetchAchievements(): List<Achievement>?
    suspend fun syncAchievements(unlocked: List<Achievement>): List<Achievement>?
}
