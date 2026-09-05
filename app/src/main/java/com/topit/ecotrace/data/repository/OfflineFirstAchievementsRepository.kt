package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.AchievementsDao
import com.topit.ecotrace.data.mapper.toDomain
import com.topit.ecotrace.data.mapper.toEntity
import com.topit.ecotrace.data.remote.AchievementsRemoteDataSource
import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.AchievementRules
import com.topit.ecotrace.domain.repository.AchievementsRepository
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class OfflineFirstAchievementsRepository @Inject constructor(
    private val achievementsDao: AchievementsDao,
    private val reportsRepository: ReportsRepository,
    private val remoteDataSource: AchievementsRemoteDataSource,
    private val authRepository: AuthRepository,
    private val clock: Clock,
) : AchievementsRepository {

    override fun observeAchievements(): Flow<List<Achievement>> {
        val userId = authRepository.currentSession()?.userId ?: return flowOf(emptyList())
        return achievementsDao.observeForUser(userId).map { stored ->
            stored.mapNotNull { it.toDomain() }
        }
    }

    override suspend fun refresh() {
        val userId = authRepository.currentSession()?.userId ?: return
        unlockLocally(userId)
        syncWithServer(userId)
    }

    override suspend fun clearLocal() {
        achievementsDao.clear()
    }

    private suspend fun unlockLocally(userId: String) {
        val myReports = reportsRepository.observeReports().first()
            .filter { it.authorId == userId }
        val storedCodes = achievementsDao.getForUser(userId).map { it.code }.toSet()
        val unlockedAt = Instant.now(clock)

        val newlyUnlocked = AchievementRules.eligible(myReports)
            .filterNot { it.name in storedCodes }
            .map { code ->
                Achievement(code = code, unlockedAt = unlockedAt, synced = false)
                    .toEntity(userId = userId, synced = false)
            }

        if (newlyUnlocked.isNotEmpty()) {
            achievementsDao.insertAll(newlyUnlocked)
        }
    }

    private suspend fun syncWithServer(userId: String) {
        val unsynced = achievementsDao.getUnsynced(userId).mapNotNull { it.toDomain() }
        val confirmed = if (unsynced.isEmpty()) {
            remoteDataSource.fetchAchievements()
        } else {
            remoteDataSource.syncAchievements(unsynced)
        } ?: return

        achievementsDao.insertAll(
            confirmed.map { it.toEntity(userId = userId, synced = true) },
        )
    }
}
