package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
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
    val error: String? = null,
)

class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(
            isAuthenticated = authRepository.currentSession() != null,
            session = authRepository.currentSession(),
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.login(email, password)
            _uiState.update {
                result.fold(
                    onSuccess = { session ->
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            session = session,
                            error = null,
                        )
                    },
                    onFailure = { error ->
                        it.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            error = error.message ?: "Login failed",
                        )
                    },
                )
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.register(name, email, password)
            _uiState.update {
                result.fold(
                    onSuccess = { session ->
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            session = session,
                            error = null,
                        )
                    },
                    onFailure = { error ->
                        it.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            error = error.message ?: "Registration failed",
                        )
                    },
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState()
    }
}
