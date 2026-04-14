package com.ecotrace.backend.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val role: UserRole,
    val createdAt: Instant,
)

enum class UserRole { USER, ADMIN }

// ── DTOs ──────────────────────────────────────────────────

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val createdAt: String,
)

fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    displayName = displayName,
    role = role.name,
    createdAt = createdAt.toString(),
)
