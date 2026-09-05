package com.ecotrace.backend.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.ecotrace.backend.auth.JwtConfig
import com.ecotrace.backend.domain.model.User
import com.ecotrace.backend.domain.model.UserRole
import com.ecotrace.backend.plugins.configureSerialization
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest {

    private val users = FakeUsersRepository(listOf(existingUser()))

    @Test
    fun register_createsUserAndReturnsToken() = testApplication {
        installAuthRoutes()

        val response = register("new@example.com", "secret123", "Ivan")

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"token\""))
        assertTrue(users.stored.values.any { it.email == "new@example.com" })
    }

    @Test
    fun register_normalisesEmailBeforeCheckingForDuplicates() = testApplication {
        installAuthRoutes()

        val response = register("  USER@Example.COM ", "secret123", "Ivan")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(1, users.stored.size)
    }

    @Test
    fun register_storesEmailInLowerCase() = testApplication {
        installAuthRoutes()

        register("New.User@Example.com", "secret123", "Ivan")

        assertTrue(users.stored.values.any { it.email == "new.user@example.com" })
    }

    @Test
    fun register_rejectsMalformedEmail() = testApplication {
        installAuthRoutes()

        assertEquals(HttpStatusCode.BadRequest, register("not-an-email", "secret123", "Ivan").status)
        assertEquals(HttpStatusCode.BadRequest, register("", "secret123", "Ivan").status)
    }

    @Test
    fun register_rejectsPasswordOutsideAllowedLength() = testApplication {
        installAuthRoutes()

        assertEquals(HttpStatusCode.BadRequest, register("a@b.com", "12345", "Ivan").status)
        assertEquals(HttpStatusCode.BadRequest, register("a@b.com", "x".repeat(73), "Ivan").status)
    }

    @Test
    fun register_rejectsEmptyOrOverlongDisplayName() = testApplication {
        installAuthRoutes()

        assertEquals(HttpStatusCode.BadRequest, register("a@b.com", "secret123", "   ").status)
        assertEquals(HttpStatusCode.BadRequest, register("a@b.com", "secret123", "n".repeat(129)).status)
    }

    @Test
    fun login_returnsTokenForValidCredentials() = testApplication {
        installAuthRoutes()

        val response = login("user@example.com", "secret123")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"token\""))
    }

    @Test
    fun login_rejectsWrongPassword() = testApplication {
        installAuthRoutes()

        assertEquals(HttpStatusCode.Unauthorized, login("user@example.com", "wrong-password").status)
    }

    @Test
    fun login_rejectsUnknownUser() = testApplication {
        installAuthRoutes()

        assertEquals(HttpStatusCode.Unauthorized, login("nobody@example.com", "secret123").status)
    }

    @Test
    fun login_acceptsEmailInAnyCase() = testApplication {
        installAuthRoutes()

        assertEquals(HttpStatusCode.OK, login("  USER@EXAMPLE.com ", "secret123").status)
    }

    private fun ApplicationTestBuilder.installAuthRoutes() {
        environment { config = testJwtConfig() }
        application {
            configureSerialization()
            installTestRateLimit()
            routing { authRoutes(users, JwtConfig(this@application)) }
        }
    }

    private suspend fun ApplicationTestBuilder.register(
        email: String,
        password: String,
        displayName: String,
    ): HttpResponse = client.post("/api/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(
            """{"email":"$email","password":"$password","displayName":"$displayName"}""",
        )
    }

    private suspend fun ApplicationTestBuilder.login(email: String, password: String): HttpResponse =
        client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }

    private companion object {
        fun existingUser() = User(
            id = "user-1",
            email = "user@example.com",
            passwordHash = BCrypt.withDefaults().hashToString(4, "secret123".toCharArray()),
            displayName = "Existing",
            role = UserRole.USER,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
    }
}
