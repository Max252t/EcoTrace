package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.remote.api.PublicUserDto
import com.topit.ecotrace.data.remote.api.UsersApi
import com.topit.ecotrace.domain.repository.AuthSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class BackendUsersRepositoryTest {

    private val usersApi: UsersApi = mockk()
    private val sessionStorage: SessionStorage = mockk()

    @Test
    fun displayName_returnsAuthorNameFromServer() = runBlocking {
        every { sessionStorage.read() } returns SESSION
        coEvery { usersApi.getUser("author-1") } returns PublicUserDto("author-1", "Мария", "USER")

        val name = repository().displayName("author-1")

        assertEquals("Мария", name)
    }

    @Test
    fun displayName_usesCurrentSessionWithoutNetworkCallForOwnReports() = runBlocking {
        every { sessionStorage.read() } returns SESSION

        val name = repository().displayName(SESSION.userId)

        assertEquals(SESSION.displayName, name)
        coVerify(exactly = 0) { usersApi.getUser(any()) }
    }

    @Test
    fun displayName_cachesResultAndQueriesServerOnce() = runBlocking {
        every { sessionStorage.read() } returns SESSION
        coEvery { usersApi.getUser("author-1") } returns PublicUserDto("author-1", "Мария", "USER")
        val repository = repository()

        repository.displayName("author-1")
        val cached = repository.displayName("author-1")

        assertEquals("Мария", cached)
        coVerify(exactly = 1) { usersApi.getUser("author-1") }
    }

    @Test
    fun displayName_returnsNullWhenServerIsUnreachable() = runBlocking {
        every { sessionStorage.read() } returns SESSION
        coEvery { usersApi.getUser("author-1") } throws IOException("offline")

        assertNull(repository().displayName("author-1"))
    }

    @Test
    fun displayName_returnsNullForSignedOutUser() = runBlocking {
        every { sessionStorage.read() } returns null

        assertNull(repository().displayName("author-1"))
        coVerify(exactly = 0) { usersApi.getUser(any()) }
    }

    @Test
    fun displayName_returnsNullForBlankAuthor() = runBlocking {
        assertNull(repository().displayName(""))
    }

    private fun repository() = BackendUsersRepository(usersApi, sessionStorage)

    private companion object {
        val SESSION = AuthSession(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "Иван",
            role = "USER",
        )
    }
}
