package com.topit.ecotrace.presentation.viewmodel

import com.topit.ecotrace.domain.repository.AuthError
import com.topit.ecotrace.domain.repository.AuthFailure
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import com.topit.ecotrace.domain.usecase.LogoutUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private val logoutUseCase: LogoutUseCase = mockk(relaxed = true)
    private val sessionFlow = MutableStateFlow<AuthSession?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authRepository.observeSession() } returns sessionFlow
        every { authRepository.currentSession() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_reflectsSessionPresentAtConstruction() = runTest {
        every { authRepository.currentSession() } returns SESSION
        sessionFlow.value = SESSION

        val viewModel = AuthViewModel(authRepository, logoutUseCase)

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals(SESSION, viewModel.uiState.value.session)
    }

    @Test
    fun uiState_updatesWhenObservedSessionChanges() = runTest {
        val viewModel = AuthViewModel(authRepository, logoutUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertTrue(!viewModel.uiState.value.isAuthenticated)

        sessionFlow.value = SESSION

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals(SESSION, viewModel.uiState.value.session)
    }

    @Test
    fun login_updatesStateToAuthenticatedOnSuccess() = runTest {
        coEvery { authRepository.login("user@example.com", "secret") } returns Result.success(SESSION)
        val viewModel = AuthViewModel(authRepository, logoutUseCase)

        viewModel.login("user@example.com", "secret")

        assertEquals(
            AuthUiState(isLoading = false, isAuthenticated = true, session = SESSION, error = null),
            viewModel.uiState.value,
        )
    }

    @Test
    fun login_setsTypedErrorOnFailure() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(AuthFailure(AuthError.INVALID_CREDENTIALS))
        val viewModel = AuthViewModel(authRepository, logoutUseCase)

        viewModel.login("user@example.com", "wrong")

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertTrue(!state.isAuthenticated)
        assertEquals(AuthError.INVALID_CREDENTIALS, state.error)
    }

    @Test
    fun login_fallsBackToServerErrorForUnknownFailure() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(IllegalStateException())
        val viewModel = AuthViewModel(authRepository, logoutUseCase)

        viewModel.login("user@example.com", "wrong")

        assertEquals(AuthError.SERVER, viewModel.uiState.value.error)
    }

    @Test
    fun register_updatesStateToAuthenticatedOnSuccess() = runTest {
        coEvery { authRepository.register("User", "user@example.com", "secret") } returns Result.success(SESSION)
        val viewModel = AuthViewModel(authRepository, logoutUseCase)

        viewModel.register("User", "user@example.com", "secret")

        assertEquals(
            AuthUiState(isLoading = false, isAuthenticated = true, session = SESSION, error = null),
            viewModel.uiState.value,
        )
    }

    @Test
    fun register_fallsBackToServerErrorForUnknownFailure() = runTest {
        coEvery { authRepository.register(any(), any(), any()) } returns Result.failure(IllegalStateException())
        val viewModel = AuthViewModel(authRepository, logoutUseCase)

        viewModel.register("User", "user@example.com", "secret")

        assertEquals(AuthError.SERVER, viewModel.uiState.value.error)
    }

    @Test
    fun logout_resetsStateAndClearsLocalData() = runTest {
        every { authRepository.currentSession() } returns SESSION
        val viewModel = AuthViewModel(authRepository, logoutUseCase)

        viewModel.logout()

        coVerify(exactly = 1) { logoutUseCase() }
        assertEquals(AuthUiState(), viewModel.uiState.value)
        assertNull(viewModel.uiState.value.session)
    }

    private companion object {
        val SESSION = AuthSession(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "User",
            role = "USER",
        )
    }
}
