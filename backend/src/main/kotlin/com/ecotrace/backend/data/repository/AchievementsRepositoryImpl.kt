package com.ecotrace.backend.data.repository

import com.ecotrace.backend.data.db.UserAchievementsTable
import com.ecotrace.backend.data.db.dbQuery
import com.ecotrace.backend.domain.model.Achievement
import com.ecotrace.backend.domain.model.AchievementCode
import com.ecotrace.backend.domain.repository.AchievementsRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert

class AchievementsRepositoryImpl : AchievementsRepository {

    override suspend fun getByUser(userId: String): List<Achievement> = dbQuery {
        UserAchievementsTable.selectAll()
            .where { UserAchievementsTable.userId eq userId }
            .mapNotNull(::rowToAchievement)
            .sortedBy { it.unlockedAt }
    }

    override suspend fun saveAll(achievements: List<Achievement>) = dbQuery {
        achievements.forEach { achievement ->
            UserAchievementsTable.upsert {
                it[userId] = achievement.userId
                it[code] = achievement.code.name
                it[unlockedAt] = achievement.unlockedAt
            }
        }
    }

    private fun rowToAchievement(row: ResultRow): Achievement? {
        val code = runCatching { AchievementCode.valueOf(row[UserAchievementsTable.code]) }.getOrNull()
            ?: return null
        return Achievement(
            userId = row[UserAchievementsTable.userId],
            code = code,
            unlockedAt = row[UserAchievementsTable.unlockedAt],
        )
    }
}
