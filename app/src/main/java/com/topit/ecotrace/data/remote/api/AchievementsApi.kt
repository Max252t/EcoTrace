package com.topit.ecotrace.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AchievementsApi {
    @GET("api/achievements")
    suspend fun getAchievements(): List<AchievementDto>

    @POST("api/achievements/sync")
    suspend fun syncAchievements(@Body request: SyncAchievementsRequestDto): List<AchievementDto>
}

data class AchievementDto(
    val code: String,
    val unlockedAt: String,
)

data class SyncAchievementsRequestDto(
    val achievements: List<AchievementDto>,
)
