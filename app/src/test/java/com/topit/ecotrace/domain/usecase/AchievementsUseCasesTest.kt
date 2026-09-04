package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.AchievementCode
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.AchievementsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AchievementsUseCasesTest {

    @Test
    fun getAchievementsUseCase_returnsAchievementsFromRepository() = runBlocking {
        val achievement = Achievement(
            code = AchievementCode.FIRST_REPORT,
            unlockedAt = Instant.parse("2026-05-01T10:00:00Z"),
            synced = true,
        )
        val repository: AchievementsRepository = mockk()
        every { repository.observeAchievements() } returns flowOf(listOf(achievement))
        val useCase = GetAchievementsUseCase(repository)

        val result = useCase().first()

        assertEquals(listOf(achievement), result)
    }

    @Test
    fun syncAchievementsUseCase_delegatesRefreshToRepository() = runBlocking {
        val repository: AchievementsRepository = mockk()
        coEvery { repository.refresh() } just Runs
        val useCase = SyncAchievementsUseCase(repository)

        useCase()

        coVerify(exactly = 1) { repository.refresh() }
    }

    @Test
    fun getUserStatsUseCase_derivesStatsFromCurrentUsersReports() = runBlocking {
        val reports = listOf(
            report(status = ReportStatus.OPEN),
            report(status = ReportStatus.RESOLVED),
        )
        val getMyReportsUseCase: GetMyReportsUseCase = mockk()
        every { getMyReportsUseCase() } returns flowOf(reports)
        val useCase = GetUserStatsUseCase(getMyReportsUseCase)

        val stats = useCase().first()

        assertEquals(2, stats.reportsSubmitted)
        assertEquals(1, stats.problemsSolved)
        assertEquals(2 * 20 + 1 * 30, stats.ecoPoints)
    }

    @Test
    fun getUserStatsUseCase_returnsZeroStatsWhenThereAreNoReports() = runBlocking {
        val getMyReportsUseCase: GetMyReportsUseCase = mockk()
        every { getMyReportsUseCase() } returns flowOf(emptyList())
        val useCase = GetUserStatsUseCase(getMyReportsUseCase)

        val stats = useCase().first()

        assertEquals(0, stats.reportsSubmitted)
        assertEquals(0, stats.problemsSolved)
        assertEquals(0, stats.ecoPoints)
    }

    private fun report(status: ReportStatus) = Report(
        id = "r-1",
        title = "title",
        description = "description",
        type = ProblemType.DUMP,
        status = status,
        latitude = 55.0,
        longitude = 37.0,
        authorId = "user-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = true,
    )
}
