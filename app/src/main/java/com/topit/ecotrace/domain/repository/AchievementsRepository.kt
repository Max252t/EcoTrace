package com.topit.ecotrace.domain.repository

import com.topit.ecotrace.domain.model.Achievement
import kotlinx.coroutines.flow.Flow

interface AchievementsRepository {
    fun observeAchievements(): Flow<List<Achievement>>
    suspend fun refresh()
    suspend fun clearLocal()
}
