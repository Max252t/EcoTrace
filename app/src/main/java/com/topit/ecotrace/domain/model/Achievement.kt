package com.topit.ecotrace.domain.model

import java.time.Instant

enum class AchievementCode {
    FIRST_REPORT,
    REPORTER_10,
    FOREST_DEFENDER,
    PROBLEM_SOLVER,
    LEVEL_5,
}

data class Achievement(
    val code: AchievementCode,
    val unlockedAt: Instant,
    val synced: Boolean = false,
)

data class UserStats(
    val reportsSubmitted: Int = 0,
    val problemsSolved: Int = 0,
    val ecoPoints: Int = 0,
) {
    val level: Int = ecoPoints / AchievementRules.POINTS_PER_LEVEL + 1
    val pointsForNextLevel: Int = level * AchievementRules.POINTS_PER_LEVEL
    val levelProgress: Float =
        (ecoPoints % AchievementRules.POINTS_PER_LEVEL) / AchievementRules.POINTS_PER_LEVEL.toFloat()
}
