package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.remote.api.UsersApi
import com.topit.ecotrace.domain.repository.UsersRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendUsersRepository @Inject constructor(
    private val usersApi: UsersApi,
    private val sessionStorage: SessionStorage,
) : UsersRepository {

    private val names = object : LinkedHashMap<String, String>(CACHE_SIZE, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > CACHE_SIZE
    }

    private var cachedFor: String? = null

    override suspend fun displayName(userId: String): String? {
        if (userId.isBlank()) return null

        val session = sessionStorage.read()
        if (session?.userId != cachedFor) {
            synchronized(names) {
                names.clear()
                cachedFor = session?.userId
            }
        }

        if (session != null && session.userId == userId) {
            return session.displayName.ifBlank { null }
        }

        synchronized(names) { names[userId] }?.let { return it }
        if (session == null) return null

        val name = runCatching { usersApi.getUser(userId).displayName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        synchronized(names) { names[userId] = name }
        return name
    }

    private companion object {
        const val CACHE_SIZE = 100
        const val LOAD_FACTOR = 0.75f
    }
}
