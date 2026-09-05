package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.repository.AuthError
import com.topit.ecotrace.domain.repository.AuthFailure
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import com.topit.ecotrace.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val session: AuthSession? = null,
    val error: AuthError? = null,
)

class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(
            isAuthenticated = authRepository.currentSession() != null,
            session = authRepository.currentSession(),
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                _uiState.update { it.copy(isAuthenticated = session != null, session = session) }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            updateWith(authRepository.login(email, password))
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            updateWith(authRepository.register(name, email, password))
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.value = AuthUiState()
        }
    }

    private fun updateWith(result: Result<AuthSession>) {
        _uiState.update { state ->
            result.fold(
                onSuccess = { session ->
                    state.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        session = session,
                        error = null,
                    )
                },
                onFailure = { error ->
                    state.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        error = (error as? AuthFailure)?.error ?: AuthError.SERVER,
                    )
                },
            )
        }
    }
}
