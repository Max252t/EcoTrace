package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.remote.api.UsersApi
import com.topit.ecotrace.domain.repository.UsersRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendUsersRepository @Inject constructor(
    private val usersApi: UsersApi,
    private val sessionStorage: SessionStorage,
) : UsersRepository {

    private val names = ConcurrentHashMap<String, String>()

    override suspend fun displayName(userId: String): String? {
        if (userId.isBlank()) return null

        val session = sessionStorage.read()
        if (session != null && session.userId == userId) {
            return session.displayName.ifBlank { null }
        }

        names[userId]?.let { return it }
        if (session == null) return null

        val name = runCatching { usersApi.getUser(userId).displayName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        names[userId] = name
        return name
    }
}
