package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.AchievementEntity
import com.topit.ecotrace.data.local.AchievementsDao
import com.topit.ecotrace.data.remote.AchievementsRemoteDataSource
import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.AchievementCode
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import com.topit.ecotrace.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OfflineFirstAchievementsRepositoryTest {

    private val unlockInstant: Instant = Instant.parse("2026-05-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(unlockInstant, ZoneOffset.UTC)

    @Test
    fun refresh_unlocksAchievementWhileOfflineAndKeepsItUnsynced() = runBlocking {
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource(offline = true)
        val repository = repository(dao, remote, reports = listOf(myReport()))

        repository.refresh()

        val stored = dao.getForUser(USER_ID)
        assertEquals(listOf(AchievementCode.FIRST_REPORT.name), stored.map { it.code })
        assertEquals(unlockInstant.epochSecond, stored.single().unlockedAtEpochSeconds)
        assertFalse(stored.single().synced)
    }

    @Test
    fun refresh_pushesOfflineUnlockToServerAndMarksItSynced() = runBlocking {
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource()
        val repository = repository(dao, remote, reports = listOf(myReport()))

        repository.refresh()

        assertEquals(
            listOf(AchievementCode.FIRST_REPORT),
            remote.syncedAchievements.map { it.code },
        )
        assertEquals(unlockInstant, remote.syncedAchievements.single().unlockedAt)
        assertTrue(dao.getForUser(USER_ID).single().synced)
        assertEquals(emptyList<AchievementEntity>(), dao.getUnsynced(USER_ID))
    }

    @Test
    fun refresh_keepsAchievementUnsyncedWhenServerIsUnreachable() = runBlocking {
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource(offline = true)
        val repository = repository(dao, remote, reports = listOf(myReport()))

        repository.refresh()
        repository.refresh()

        assertEquals(1, dao.getUnsynced(USER_ID).size)
        assertEquals(unlockInstant.epochSecond, dao.getForUser(USER_ID).single().unlockedAtEpochSeconds)
    }

    @Test
    fun refresh_retriesPushOnceServerIsReachableAgain() = runBlocking {
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource(offline = true)
        val repository = repository(dao, remote, reports = listOf(myReport()))

        repository.refresh()
        remote.offline = false
        repository.refresh()

        assertEquals(unlockInstant, remote.syncedAchievements.single().unlockedAt)
        assertTrue(dao.getForUser(USER_ID).single().synced)
    }

    @Test
    fun refresh_pullsServerAchievementsWhenThereIsNothingToPush() = runBlocking {
        val unlockedElsewhere = Achievement(
            code = AchievementCode.PROBLEM_SOLVER,
            unlockedAt = Instant.parse("2026-04-01T00:00:00Z"),
            synced = true,
        )
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource(serverAchievements = listOf(unlockedElsewhere))
        val repository = repository(dao, remote, reports = emptyList())

        repository.refresh()

        assertEquals(1, remote.fetchCalls)
        assertEquals(0, remote.syncCalls)
        val stored = dao.getForUser(USER_ID).single()
        assertEquals(AchievementCode.PROBLEM_SOLVER.name, stored.code)
        assertTrue(stored.synced)
    }

    @Test
    fun refresh_keepsOriginalUnlockTimeOnRepeatedRuns() = runBlocking {
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource(offline = true)
        val repository = repository(dao, remote, reports = listOf(myReport()))

        repository.refresh()
        val laterRepository = repository(
            dao = dao,
            remote = remote,
            reports = listOf(myReport(), myReport(id = "report-2")),
            clock = Clock.fixed(unlockInstant.plusSeconds(3600), ZoneOffset.UTC),
        )
        laterRepository.refresh()

        assertEquals(1, dao.getForUser(USER_ID).size)
        assertEquals(unlockInstant.epochSecond, dao.getForUser(USER_ID).single().unlockedAtEpochSeconds)
    }

    @Test
    fun refresh_ignoresReportsOfOtherUsers() = runBlocking {
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource(offline = true)
        val repository = repository(
            dao = dao,
            remote = remote,
            reports = listOf(myReport(authorId = "someone-else")),
        )

        repository.refresh()

        assertEquals(emptyList<AchievementEntity>(), dao.getForUser(USER_ID))
    }

    @Test
    fun refresh_doesNothingForSignedOutUser() = runBlocking {
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource()
        val repository = repository(dao, remote, reports = listOf(myReport()), session = null)

        repository.refresh()

        assertEquals(emptyList<AchievementEntity>(), dao.getForUser(USER_ID))
        assertEquals(0, remote.syncCalls)
        assertEquals(0, remote.fetchCalls)
    }

    @Test
    fun observeAchievements_emitsStoredAchievementsOfCurrentUser() = runBlocking {
        val dao = FakeAchievementsDao()
        val remote = FakeAchievementsRemoteDataSource(offline = true)
        val repository = repository(dao, remote, reports = listOf(myReport()))

        repository.refresh()

        val observed = repository.observeAchievements().first()
        assertEquals(listOf(AchievementCode.FIRST_REPORT), observed.map { it.code })
        assertEquals(unlockInstant, observed.single().unlockedAt)
    }

    @Test
    fun observeAchievements_isEmptyForSignedOutUser() = runBlocking {
        val repository = repository(
            dao = FakeAchievementsDao(),
            remote = FakeAchievementsRemoteDataSource(),
            reports = emptyList(),
            session = null,
        )

        assertEquals(emptyList<Achievement>(), repository.observeAchievements().first())
    }

    @Test
    fun observeAchievements_skipsUnknownAchievementCodes() = runBlocking {
        val dao = FakeAchievementsDao()
        dao.insertAll(
            listOf(
                AchievementEntity(USER_ID, "SOMETHING_NEW", unlockInstant.epochSecond, synced = true),
                AchievementEntity(
                    USER_ID,
                    AchievementCode.FIRST_REPORT.name,
                    unlockInstant.epochSecond,
                    synced = true,
                ),
            ),
        )
        val repository = repository(dao, FakeAchievementsRemoteDataSource(), reports = emptyList())

        val observed = repository.observeAchievements().first()

        assertEquals(listOf(AchievementCode.FIRST_REPORT), observed.map { it.code })
    }

    private fun repository(
        dao: AchievementsDao,
        remote: AchievementsRemoteDataSource,
        reports: List<Report>,
        session: AuthSession? = SESSION,
        clock: Clock = this.clock,
    ) = OfflineFirstAchievementsRepository(
        achievementsDao = dao,
        reportsRepository = FakeReportsRepository(reports),
        remoteDataSource = remote,
        authRepository = FakeAuthRepository(session),
        clock = clock,
    )

    private fun myReport(
        id: String = "report-1",
        authorId: String = USER_ID,
        type: ProblemType = ProblemType.DUMP,
        status: ReportStatus = ReportStatus.OPEN,
    ) = Report(
        id = id,
        title = "title",
        description = "description",
        type = type,
        status = status,
        latitude = 55.0,
        longitude = 37.0,
        authorId = authorId,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = true,
    )

    private companion object {
        const val USER_ID = "user-1"
        val SESSION = AuthSession(
            token = "token",
            userId = USER_ID,
            email = "user@example.com",
            displayName = "User",
            role = "USER",
        )
    }
}

private class FakeAchievementsDao : AchievementsDao {
    private val rows = MutableStateFlow<List<AchievementEntity>>(emptyList())

    override fun observeForUser(userId: String): Flow<List<AchievementEntity>> =
        rows.map { all -> all.filter { it.userId == userId }.sortedBy { it.unlockedAtEpochSeconds } }

    override suspend fun getForUser(userId: String): List<AchievementEntity> =
        rows.value.filter { it.userId == userId }

    override suspend fun getUnsynced(userId: String): List<AchievementEntity> =
        rows.value.filter { it.userId == userId && !it.synced }

    override suspend fun insertAll(achievements: List<AchievementEntity>) {
        val updated = rows.value.toMutableList()
        achievements.forEach { achievement ->
            updated.removeAll { it.userId == achievement.userId && it.code == achievement.code }
            updated += achievement
        }
        rows.value = updated
    }
}

private class FakeAchievementsRemoteDataSource(
    var offline: Boolean = false,
    private val serverAchievements: List<Achievement> = emptyList(),
) : AchievementsRemoteDataSource {

    var fetchCalls: Int = 0
        private set

    var syncCalls: Int = 0
        private set

    var syncedAchievements: List<Achievement> = emptyList()
        private set

    override suspend fun fetchAchievements(): List<Achievement>? {
        fetchCalls += 1
        return if (offline) null else serverAchievements
    }

    override suspend fun syncAchievements(unlocked: List<Achievement>): List<Achievement>? {
        syncCalls += 1
        if (offline) return null
        syncedAchievements = unlocked
        return (serverAchievements + unlocked).map { it.copy(synced = true) }
    }
}

private class FakeReportsRepository(private val reports: List<Report>) : ReportsRepository {
    override fun observeReports(): Flow<List<Report>> = MutableStateFlow(reports)
    override suspend fun getReportById(id: String): Report? = reports.firstOrNull { it.id == id }
    override suspend fun createReport(report: Report) = Unit
    override suspend fun markAsResolved(id: String) = Unit
    override suspend fun deleteReport(id: String) = Unit
    override suspend fun syncPending() = Unit
}

private class FakeAuthRepository(private val session: AuthSession?) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<AuthSession> =
        Result.failure(UnsupportedOperationException())

    override suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<AuthSession> = Result.failure(UnsupportedOperationException())

    override fun currentSession(): AuthSession? = session

    override fun observeSession(): Flow<AuthSession?> = MutableStateFlow(session)

    override fun logout() = Unit
}
