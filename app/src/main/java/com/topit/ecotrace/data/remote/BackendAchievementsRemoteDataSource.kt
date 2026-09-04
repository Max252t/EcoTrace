package com.topit.ecotrace.data.remote

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.mapper.toDomain
import com.topit.ecotrace.data.mapper.toDto
import com.topit.ecotrace.data.remote.api.AchievementsApi
import com.topit.ecotrace.data.remote.api.SyncAchievementsRequestDto
import com.topit.ecotrace.domain.model.Achievement
import javax.inject.Inject

class BackendAchievementsRemoteDataSource @Inject constructor(
    private val achievementsApi: AchievementsApi,
    private val sessionStorage: SessionStorage,
) : AchievementsRemoteDataSource {

    override suspend fun fetchAchievements(): List<Achievement>? {
        if (sessionStorage.token().isNullOrBlank()) return null
        return runCatching {
            achievementsApi.getAchievements().mapNotNull { it.toDomain() }
        }.getOrNull()
    }

    override suspend fun syncAchievements(unlocked: List<Achievement>): List<Achievement>? {
        if (sessionStorage.token().isNullOrBlank()) return null
        return runCatching {
            achievementsApi
                .syncAchievements(SyncAchievementsRequestDto(unlocked.map { it.toDto() }))
                .mapNotNull { it.toDomain() }
        }.getOrNull()
    }
}
