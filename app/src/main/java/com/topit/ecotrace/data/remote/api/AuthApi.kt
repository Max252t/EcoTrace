package com.topit.ecotrace.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto
}

data class LoginRequestDto(
    val email: String,
    val password: String,
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val displayName: String,
)

data class AuthResponseDto(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
)
