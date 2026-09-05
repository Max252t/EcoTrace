package com.topit.ecotrace.presentation.viewmodel

import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import io.mockk.Runs
import io.mockk.coEvery
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

        val viewModel = AuthViewModel(authRepository)

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals(SESSION, viewModel.uiState.value.session)
    }

    @Test
    fun uiState_updatesWhenObservedSessionChanges() = runTest {
        val viewModel = AuthViewModel(authRepository)
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
        val viewModel = AuthViewModel(authRepository)

        viewModel.login("user@example.com", "secret")

        assertEquals(
            AuthUiState(isLoading = false, isAuthenticated = true, session = SESSION, error = null),
            viewModel.uiState.value,
        )
    }

    @Test
    fun login_setsErrorMessageOnFailure() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(IllegalStateException("Неверный email или пароль"))
        val viewModel = AuthViewModel(authRepository)

        viewModel.login("user@example.com", "wrong")

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertTrue(!state.isAuthenticated)
        assertEquals("Неверный email или пароль", state.error)
    }

    @Test
    fun login_fallsBackToDefaultMessageWhenErrorHasNone() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(IllegalStateException())
        val viewModel = AuthViewModel(authRepository)

        viewModel.login("user@example.com", "wrong")

        assertEquals("Login failed", viewModel.uiState.value.error)
    }

    @Test
    fun register_updatesStateToAuthenticatedOnSuccess() = runTest {
        coEvery { authRepository.register("User", "user@example.com", "secret") } returns Result.success(SESSION)
        val viewModel = AuthViewModel(authRepository)

        viewModel.register("User", "user@example.com", "secret")

        assertEquals(
            AuthUiState(isLoading = false, isAuthenticated = true, session = SESSION, error = null),
            viewModel.uiState.value,
        )
    }

    @Test
    fun register_fallsBackToDefaultMessageWhenErrorHasNone() = runTest {
        coEvery { authRepository.register(any(), any(), any()) } returns Result.failure(IllegalStateException())
        val viewModel = AuthViewModel(authRepository)

        viewModel.register("User", "user@example.com", "secret")

        assertEquals("Registration failed", viewModel.uiState.value.error)
    }

    @Test
    fun logout_resetsStateAndDelegatesToRepository() = runTest {
        every { authRepository.currentSession() } returns SESSION
        every { authRepository.logout() } just Runs
        val viewModel = AuthViewModel(authRepository)

        viewModel.logout()

        verify(exactly = 1) { authRepository.logout() }
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
