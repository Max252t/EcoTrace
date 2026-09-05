package com.ecotrace.backend.routes

import com.ecotrace.backend.domain.model.AchievementCode
import com.ecotrace.backend.domain.model.ProblemType
import com.ecotrace.backend.domain.model.Report
import com.ecotrace.backend.domain.model.ReportStatus
import com.ecotrace.backend.domain.model.User
import com.ecotrace.backend.domain.model.UserRole
import com.ecotrace.backend.plugins.configureSerialization
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementsRoutesTest {

    private val achievements = FakeAchievementsRepository()
    private val reports = FakeReportsRepository(listOf(reportOf(USER_ID)))
    private val users = FakeUsersRepository(listOf(user()))

    @Test
    fun sync_storesOfflineUnlockConfirmedByServerData() = testApplication {
        installAchievementRoutes()

        val response = sync(USER_ID, AchievementCode.FIRST_REPORT, "2026-02-01T00:00:00Z")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("FIRST_REPORT"))
        assertEquals(
            Instant.parse("2026-02-01T00:00:00Z"),
            achievements.stored.getValue(USER_ID).single().unlockedAt,
        )
    }

    @Test
    fun sync_ignoresClaimThatServerDataDoesNotConfirm() = testApplication {
        installAchievementRoutes()

        val response = sync(USER_ID, AchievementCode.LEVEL_5, "2026-02-01T00:00:00Z")

        assertEquals(HttpStatusCode.OK, response.status)
        assertFalse(response.bodyAsText().contains("LEVEL_5"))
    }

    @Test
    fun sync_clampsUnlockTimeToAccountCreation() = testApplication {
        installAchievementRoutes()

        sync(USER_ID, AchievementCode.FIRST_REPORT, "1970-01-01T00:00:00Z")

        assertEquals(
            user().createdAt,
            achievements.stored.getValue(USER_ID).single().unlockedAt,
        )
    }

    @Test
    fun sync_rejectsUnknownAchievementCode() = testApplication {
        installAchievementRoutes()

        val response = client.post("/api/achievements/sync") {
            header(HttpHeaders.Authorization, "Bearer ${testToken(USER_ID)}")
            contentType(ContentType.Application.Json)
            setBody("""{"achievements":[{"code":"MADE_UP","unlockedAt":"2026-02-01T00:00:00Z"}]}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun sync_rejectsMalformedUnlockTime() = testApplication {
        installAchievementRoutes()

        val response = client.post("/api/achievements/sync") {
            header(HttpHeaders.Authorization, "Bearer ${testToken(USER_ID)}")
            contentType(ContentType.Application.Json)
            setBody("""{"achievements":[{"code":"FIRST_REPORT","unlockedAt":"yesterday"}]}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun get_requiresAuthentication() = testApplication {
        installAchievementRoutes()

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/achievements").status)
    }

    @Test
    fun get_returnsOnlyAchievementsOfCurrentUser() = testApplication {
        installAchievementRoutes()
        sync(USER_ID, AchievementCode.FIRST_REPORT, "2026-02-01T00:00:00Z")

        val body = client.get("/api/achievements") {
            header(HttpHeaders.Authorization, "Bearer ${testToken("someone-else")}")
        }.bodyAsText()

        assertFalse(body.contains("FIRST_REPORT"))
    }

    private fun ApplicationTestBuilder.installAchievementRoutes() {
        application {
            configureSerialization()
            installTestAuth()
            routing { achievementsRoutes(achievements, reports, users) }
        }
    }

    private suspend fun ApplicationTestBuilder.sync(
        userId: String,
        code: AchievementCode,
        unlockedAt: String,
    ): HttpResponse = client.post("/api/achievements/sync") {
        header(HttpHeaders.Authorization, "Bearer ${testToken(userId)}")
        contentType(ContentType.Application.Json)
        setBody("""{"achievements":[{"code":"${code.name}","unlockedAt":"$unlockedAt"}]}""")
    }

    private companion object {
        const val USER_ID = "user-1"

        fun user() = User(
            id = USER_ID,
            email = "user@example.com",
            passwordHash = "hash",
            displayName = "Ivan",
            role = UserRole.USER,
            createdAt = Instant.parse("2026-01-15T00:00:00Z"),
        )

        fun reportOf(authorId: String) = Report(
            id = "report-1",
            title = "title",
            description = "description",
            type = ProblemType.DUMP,
            status = ReportStatus.OPEN,
            latitude = 55.0,
            longitude = 37.0,
            imageUrl = null,
            authorId = authorId,
            createdAt = Instant.parse("2026-01-20T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-20T00:00:00Z"),
        )
    }
}
