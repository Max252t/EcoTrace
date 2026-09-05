package com.topit.ecotrace.data.remote

import com.topit.ecotrace.data.local.SessionStorage
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    private val sessionStorage: SessionStorage = mockk(relaxed = true)
    private val chain: Interceptor.Chain = mockk()

    @Test
    fun intercept_addsBearerTokenOfCurrentSession() {
        every { sessionStorage.token() } returns TOKEN
        val sent = givenResponse(code = 200)

        AuthInterceptor(sessionStorage).intercept(chain)

        assertEquals("Bearer $TOKEN", sent.captured.header("Authorization"))
    }

    @Test
    fun intercept_sendsRequestWithoutTokenForSignedOutUser() {
        every { sessionStorage.token() } returns null
        val sent = givenResponse(code = 200)

        AuthInterceptor(sessionStorage).intercept(chain)

        assertNull(sent.captured.header("Authorization"))
    }

    @Test
    fun intercept_clearsExpiredSessionOnUnauthorizedResponse() {
        every { sessionStorage.token() } returns TOKEN
        givenResponse(code = 401)

        AuthInterceptor(sessionStorage).intercept(chain)

        verify(exactly = 1) { sessionStorage.clear() }
    }

    @Test
    fun intercept_keepsSessionOnSuccessfulResponse() {
        every { sessionStorage.token() } returns TOKEN
        givenResponse(code = 200)

        AuthInterceptor(sessionStorage).intercept(chain)

        verify(exactly = 0) { sessionStorage.clear() }
    }

    @Test
    fun intercept_keepsSessionWhenLoginItselfIsRejected() {
        every { sessionStorage.token() } returns null
        givenResponse(code = 401)

        AuthInterceptor(sessionStorage).intercept(chain)

        verify(exactly = 0) { sessionStorage.clear() }
    }

    private fun givenResponse(code: Int): CapturingSlot<Request> {
        val request = Request.Builder().url("http://localhost:8080/api/reports").build()
        val sent = slot<Request>()
        every { chain.request() } returns request
        every { chain.proceed(capture(sent)) } answers {
            Response.Builder()
                .request(sent.captured)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .body("".toResponseBody(null))
                .build()
        }
        return sent
    }

    private companion object {
        const val TOKEN = "jwt-token"
    }
}
