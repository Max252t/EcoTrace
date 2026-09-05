package com.topit.ecotrace.domain.repository

import kotlinx.coroutines.flow.Flow

data class AuthSession(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
)

enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_TAKEN,
    INVALID_DATA,
    TOO_MANY_ATTEMPTS,
    SERVER,
    NETWORK,
}

class AuthFailure(val error: AuthError) : Exception(error.name)

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthSession>
    suspend fun register(name: String, email: String, password: String): Result<AuthSession>
    fun currentSession(): AuthSession?
    fun observeSession(): Flow<AuthSession?>
    fun logout()
}
