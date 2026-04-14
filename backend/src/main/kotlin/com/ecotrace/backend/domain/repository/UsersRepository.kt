package com.ecotrace.backend.domain.repository

import com.ecotrace.backend.domain.model.User

interface UsersRepository {
    suspend fun findByEmail(email: String): User?
    suspend fun findById(id: String): User?
    suspend fun create(user: User): User
    suspend fun existsByEmail(email: String): Boolean
}
