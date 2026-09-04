package com.topit.ecotrace.presentation.viewmodel

import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.AchievementCode
import com.topit.ecotrace.domain.model.UserStats
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import com.topit.ecotrace.domain.usecase.GetAchievementsUseCase
import com.topit.ecotrace.domain.usecase.GetUserStatsUseCase
import com.topit.ecotrace.domain.usecase.SyncAchievementsUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val getUserStatsUseCase: GetUserStatsUseCase = mockk()
    private val getAchievementsUseCase: GetAchievementsUseCase = mockk()
    private val syncAchievementsUseCase: SyncAchievementsUseCase = mockk()
    private val authRepository: AuthRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { syncAchievementsUseCase() } just Runs
        every { authRepository.currentSession() } returns SESSION
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_combinesProfileNameStatsAndAchievements() = runTest {
        val stats = UserStats(reportsSubmitted = 3, problemsSolved = 1, ecoPoints = 90)
        val achievement = Achievement(
            code = AchievementCode.FIRST_REPORT,
            unlockedAt = Instant.parse("2026-05-01T10:00:00Z"),
            synced = true,
        )
        every { getUserStatsUseCase() } returns flowOf(stats)
        every { getAchievementsUseCase() } returns flowOf(listOf(achievement))

        val viewModel = ProfileViewModel(
            getUserStatsUseCase,
            getAchievementsUseCase,
            syncAchievementsUseCase,
            authRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(
            ProfileUiState(
                displayName = SESSION.displayName,
                stats = stats,
                achievements = listOf(achievement),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun init_synchronisesAchievementsOnce() = runTest {
        every { getUserStatsUseCase() } returns flowOf(UserStats())
        every { getAchievementsUseCase() } returns flowOf(emptyList())

        ProfileViewModel(
            getUserStatsUseCase,
            getAchievementsUseCase,
            syncAchievementsUseCase,
            authRepository,
        )

        coVerify(exactly = 1) { syncAchievementsUseCase() }
    }

    @Test
    fun refresh_synchronisesAchievementsAgain() = runTest {
        every { getUserStatsUseCase() } returns flowOf(UserStats())
        every { getAchievementsUseCase() } returns flowOf(emptyList())

        val viewModel = ProfileViewModel(
            getUserStatsUseCase,
            getAchievementsUseCase,
            syncAchievementsUseCase,
            authRepository,
        )
        viewModel.refresh()

        coVerify(exactly = 2) { syncAchievementsUseCase() }
    }

    @Test
    fun uiState_updatesWhenNewAchievementIsUnlocked() = runTest {
        val achievements = MutableStateFlow(emptyList<Achievement>())
        every { getUserStatsUseCase() } returns flowOf(UserStats())
        every { getAchievementsUseCase() } returns achievements

        val viewModel = ProfileViewModel(
            getUserStatsUseCase,
            getAchievementsUseCase,
            syncAchievementsUseCase,
            authRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(emptyList<Achievement>(), viewModel.uiState.value.achievements)

        val unlocked = Achievement(
            code = AchievementCode.PROBLEM_SOLVER,
            unlockedAt = Instant.parse("2026-05-02T10:00:00Z"),
            synced = false,
        )
        achievements.value = listOf(unlocked)

        assertEquals(listOf(unlocked), viewModel.uiState.value.achievements)
    }

    @Test
    fun uiState_fallsBackToEmptyNameForSignedOutUser() = runTest {
        every { authRepository.currentSession() } returns null
        every { getUserStatsUseCase() } returns flowOf(UserStats())
        every { getAchievementsUseCase() } returns flowOf(emptyList())

        val viewModel = ProfileViewModel(
            getUserStatsUseCase,
            getAchievementsUseCase,
            syncAchievementsUseCase,
            authRepository,
        )

        assertEquals("", viewModel.uiState.value.displayName)
    }

    private companion object {
        val SESSION = AuthSession(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "Иван",
            role = "USER",
        )
    }
}
