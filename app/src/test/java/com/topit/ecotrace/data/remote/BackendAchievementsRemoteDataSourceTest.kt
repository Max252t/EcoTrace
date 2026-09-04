package com.topit.ecotrace.data.remote

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.remote.api.AchievementDto
import com.topit.ecotrace.data.remote.api.AchievementsApi
import com.topit.ecotrace.data.remote.api.SyncAchievementsRequestDto
import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.AchievementCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.Instant

class BackendAchievementsRemoteDataSourceTest {

    private val achievementsApi: AchievementsApi = mockk()
    private val sessionStorage: SessionStorage = mockk()

    @Test
    fun fetchAchievements_returnsNullForSignedOutUser() = runBlocking {
        every { sessionStorage.token() } returns null

        assertNull(dataSource().fetchAchievements())
        coVerify(exactly = 0) { achievementsApi.getAchievements() }
    }

    @Test
    fun fetchAchievements_returnsMappedAchievementsFromServer() = runBlocking {
        every { sessionStorage.token() } returns "token"
        coEvery { achievementsApi.getAchievements() } returns listOf(
            AchievementDto(code = AchievementCode.FIRST_REPORT.name, unlockedAt = "2026-05-01T10:00:00Z"),
        )

        val result = dataSource().fetchAchievements()

        assertEquals(
            listOf(
                Achievement(
                    code = AchievementCode.FIRST_REPORT,
                    unlockedAt = Instant.parse("2026-05-01T10:00:00Z"),
                    synced = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun fetchAchievements_skipsUnknownCodesFromServer() = runBlocking {
        every { sessionStorage.token() } returns "token"
        coEvery { achievementsApi.getAchievements() } returns listOf(
            AchievementDto(code = "SOMETHING_NEW", unlockedAt = "2026-05-01T10:00:00Z"),
            AchievementDto(code = AchievementCode.LEVEL_5.name, unlockedAt = "2026-05-01T10:00:00Z"),
        )

        val result = dataSource().fetchAchievements()

        assertEquals(listOf(AchievementCode.LEVEL_5), result?.map { it.code })
    }

    @Test
    fun fetchAchievements_returnsNullWhenServerIsUnreachable() = runBlocking {
        every { sessionStorage.token() } returns "token"
        coEvery { achievementsApi.getAchievements() } throws IOException("offline")

        assertNull(dataSource().fetchAchievements())
    }

    @Test
    fun syncAchievements_returnsNullForSignedOutUser() = runBlocking {
        every { sessionStorage.token() } returns null

        assertNull(dataSource().syncAchievements(listOf(achievement())))
        coVerify(exactly = 0) { achievementsApi.syncAchievements(any()) }
    }

    @Test
    fun syncAchievements_sendsUnlockedAchievementsAndReturnsMappedResponse() = runBlocking {
        every { sessionStorage.token() } returns "token"
        val unlocked = achievement()
        coEvery {
            achievementsApi.syncAchievements(
                SyncAchievementsRequestDto(listOf(AchievementDto(unlocked.code.name, unlocked.unlockedAt.toString()))),
            )
        } returns listOf(AchievementDto(unlocked.code.name, unlocked.unlockedAt.toString()))

        val result = dataSource().syncAchievements(listOf(unlocked))

        assertEquals(listOf(unlocked.code), result?.map { it.code })
    }

    @Test
    fun syncAchievements_returnsNullWhenServerIsUnreachable() = runBlocking {
        every { sessionStorage.token() } returns "token"
        coEvery { achievementsApi.syncAchievements(any()) } throws IOException("offline")

        assertNull(dataSource().syncAchievements(listOf(achievement())))
    }

    private fun achievement() = Achievement(
        code = AchievementCode.PROBLEM_SOLVER,
        unlockedAt = Instant.parse("2026-05-01T10:00:00Z"),
        synced = false,
    )

    private fun dataSource() = BackendAchievementsRemoteDataSource(achievementsApi, sessionStorage)
}
