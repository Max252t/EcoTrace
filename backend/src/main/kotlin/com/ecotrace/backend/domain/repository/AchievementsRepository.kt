package com.ecotrace.backend.domain.repository

import com.ecotrace.backend.domain.model.Achievement

interface AchievementsRepository {
    suspend fun getByUser(userId: String): List<Achievement>
    suspend fun saveAll(achievements: List<Achievement>)
}
