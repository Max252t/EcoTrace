package com.topit.ecotrace.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Path

interface UsersApi {
    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: String): PublicUserDto
}

data class PublicUserDto(
    val id: String,
    val displayName: String,
    val role: String,
)
