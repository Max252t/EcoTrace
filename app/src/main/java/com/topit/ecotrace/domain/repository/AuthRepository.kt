package com.topit.ecotrace.domain.repository

data class AuthSession(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
)

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthSession>
    suspend fun register(name: String, email: String, password: String): Result<AuthSession>
    fun currentSession(): AuthSession?
    fun logout()
}
