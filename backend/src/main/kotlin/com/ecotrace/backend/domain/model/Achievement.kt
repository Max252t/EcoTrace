package com.ecotrace.backend.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

data class Achievement(
    val userId: String,
    val code: AchievementCode,
    val unlockedAt: Instant,
)

enum class AchievementCode {
    FIRST_REPORT,
    REPORTER_10,
    FOREST_DEFENDER,
    PROBLEM_SOLVER,
    LEVEL_5,
}

@Serializable
data class AchievementDto(
    val code: String,
    val unlockedAt: String,
)

@Serializable
data class SyncAchievementsRequest(
    val achievements: List<AchievementDto> = emptyList(),
)

fun Achievement.toResponse() = AchievementDto(
    code = code.name,
    unlockedAt = unlockedAt.toString(),
)
