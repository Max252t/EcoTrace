package com.topit.ecotrace.domain.repository

interface UsersRepository {
    suspend fun displayName(userId: String): String?
}
