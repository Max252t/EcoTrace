package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.remote.api.AuthApi
import com.topit.ecotrace.data.remote.api.AuthResponseDto
import com.topit.ecotrace.data.remote.api.LoginRequestDto
import com.topit.ecotrace.data.remote.api.RegisterRequestDto
import com.topit.ecotrace.domain.repository.AuthSession
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class BackendAuthRepositoryTest {

    private val authApi: AuthApi = mockk()
    private val sessionStorage: SessionStorage = mockk()

    @Test
    fun login_savesSessionAndReturnsItOnSuccess() = runBlocking {
        coEvery { authApi.login(LoginRequestDto(" user@example.com ".trim(), "secret")) } returns AUTH_RESPONSE
        every { sessionStorage.save(any()) } just Runs

        val result = repository().login(" user@example.com ", "secret")

        assertTrue(result.isSuccess)
        assertEquals(SESSION, result.getOrNull())
        verify(exactly = 1) { sessionStorage.save(SESSION) }
    }

    @Test
    fun login_mapsUnauthorizedResponseToRussianMessage() = runBlocking {
        coEvery { authApi.login(any()) } throws httpException(401)

        val result = repository().login("user@example.com", "wrong")

        assertEquals("Неверный email или пароль", result.exceptionOrNull()?.message)
    }

    @Test
    fun login_mapsNetworkErrorToRussianMessage() = runBlocking {
        coEvery { authApi.login(any()) } throws IOException("offline")

        val result = repository().login("user@example.com", "secret")

        assertEquals(
            "Не удалось подключиться к серверу. Проверьте IP/порт backend",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun register_trimsInputSavesSessionAndReturnsItOnSuccess() = runBlocking {
        coEvery {
            authApi.register(RegisterRequestDto(email = "user@example.com", password = "secret", displayName = "User"))
        } returns AUTH_RESPONSE
        every { sessionStorage.save(any()) } just Runs

        val result = repository().register(" User ", " user@example.com ", "secret")

        assertTrue(result.isSuccess)
        assertEquals(SESSION, result.getOrNull())
    }

    @Test
    fun register_mapsConflictResponseToRussianMessage() = runBlocking {
        coEvery { authApi.register(any()) } throws httpException(409)

        val result = repository().register("User", "user@example.com", "secret")

        assertEquals("Этот email уже зарегистрирован", result.exceptionOrNull()?.message)
    }

    @Test
    fun register_mapsBadRequestResponseToRussianMessage() = runBlocking {
        coEvery { authApi.register(any()) } throws httpException(400)

        val result = repository().register("User", "user@example.com", "secret")

        assertEquals("Проверьте корректность введенных данных", result.exceptionOrNull()?.message)
    }

    @Test
    fun register_mapsOtherHttpErrorsToServerErrorMessage() = runBlocking {
        coEvery { authApi.register(any()) } throws httpException(500)

        val result = repository().register("User", "user@example.com", "secret")

        assertEquals("Ошибка сервера: 500", result.exceptionOrNull()?.message)
    }

    @Test
    fun currentSession_returnsSessionFromStorage() {
        every { sessionStorage.read() } returns SESSION

        assertEquals(SESSION, repository().currentSession())
    }

    @Test
    fun currentSession_returnsNullWhenSignedOut() {
        every { sessionStorage.read() } returns null

        assertNull(repository().currentSession())
    }

    @Test
    fun observeSession_delegatesToStorage() {
        val sessionFlow = MutableStateFlow<AuthSession?>(SESSION)
        every { sessionStorage.session } returns sessionFlow

        assertEquals(sessionFlow, repository().observeSession())
    }

    @Test
    fun logout_clearsStorage() {
        every { sessionStorage.clear() } just Runs

        repository().logout()

        verify(exactly = 1) { sessionStorage.clear() }
    }

    private fun httpException(code: Int): HttpException {
        val body = "".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }

    private fun repository() = BackendAuthRepository(authApi, sessionStorage)

    private companion object {
        val AUTH_RESPONSE = AuthResponseDto(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "User",
            role = "USER",
        )
        val SESSION = AuthSession(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "User",
            role = "USER",
        )
    }
}
