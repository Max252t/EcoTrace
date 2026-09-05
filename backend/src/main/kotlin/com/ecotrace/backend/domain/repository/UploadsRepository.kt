package com.ecotrace.backend.domain.repository

interface UploadsRepository {
    suspend fun record(name: String, userId: String)
    suspend fun ownerOf(name: String): String?
    suspend fun delete(name: String)
}
