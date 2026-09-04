package com.topit.ecotrace.presentation.viewmodel

import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private val session = MutableStateFlow<AuthSession?>(SESSION)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authRepository.currentSession() } answers { session.value }
        every { authRepository.observeSession() } returns session
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_isAuthenticatedForStoredSession() = runTest {
        val viewModel = AuthViewModel(authRepository)

        assertTrue(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun uiState_signsUserOutWhenSessionExpires() = runTest {
        val viewModel = AuthViewModel(authRepository)

        session.value = null

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertNull(viewModel.uiState.value.session)
    }

    @Test
    fun uiState_followsNewSessionAfterSignIn() = runTest {
        session.value = null
        val viewModel = AuthViewModel(authRepository)
        assertFalse(viewModel.uiState.value.isAuthenticated)

        session.value = SESSION

        assertTrue(viewModel.uiState.value.isAuthenticated)
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
