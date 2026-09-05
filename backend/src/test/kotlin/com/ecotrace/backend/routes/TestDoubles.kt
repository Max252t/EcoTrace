package com.ecotrace.backend.routes

import com.ecotrace.backend.domain.model.Achievement
import com.ecotrace.backend.domain.model.ProblemType
import com.ecotrace.backend.domain.model.Report
import com.ecotrace.backend.domain.model.ReportStatus
import com.ecotrace.backend.domain.model.User
import com.ecotrace.backend.domain.repository.AchievementsRepository
import com.ecotrace.backend.domain.repository.ReportsRepository
import com.ecotrace.backend.domain.repository.UploadsRepository
import com.ecotrace.backend.domain.repository.UsersRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.ratelimit.RateLimit
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

fun testJwtConfig() = MapApplicationConfig(
    "jwt.secret" to JWT_TEST_SECRET,
    "jwt.issuer" to JWT_TEST_ISSUER,
    "jwt.audience" to "ecotrace-users",
    "jwt.realm" to "EcoTrace API",
    "jwt.expiresInMs" to "86400000",
)

const val JWT_TEST_SECRET = "test-secret"
const val JWT_TEST_ISSUER = "ecotrace"

fun Application.installTestAuth() {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JWT.require(Algorithm.HMAC256(JWT_TEST_SECRET)).withIssuer(JWT_TEST_ISSUER).build())
            validate { credential -> JWTPrincipal(credential.payload) }
        }
    }
}

fun testToken(userId: String, role: String = "USER"): String = JWT.create()
    .withIssuer(JWT_TEST_ISSUER)
    .withClaim("userId", userId)
    .withClaim("role", role)
    .sign(Algorithm.HMAC256(JWT_TEST_SECRET))

fun Application.installTestRateLimit() {
    install(RateLimit) {
        register(AUTH_RATE_LIMIT) {
            rateLimiter(limit = 1000, refillPeriod = 60.seconds)
        }
    }
}

class FakeUsersRepository(users: List<User> = emptyList()) : UsersRepository {
    val stored = users.associateBy { it.id }.toMutableMap()

    override suspend fun findByEmail(email: String): User? = stored.values.firstOrNull { it.email == email }

    override suspend fun findById(id: String): User? = stored[id]

    override suspend fun create(user: User): User {
        stored[user.id] = user
        return user
    }

    override suspend fun existsByEmail(email: String): Boolean = findByEmail(email) != null
}

class FakeReportsRepository(reports: List<Report> = emptyList()) : ReportsRepository {
    val stored = reports.associateBy { it.id }.toMutableMap()

    override suspend fun getAll(
        type: ProblemType?,
        status: ReportStatus?,
        limit: Int,
        offset: Long,
    ): List<Report> = stored.values
        .filter { type == null || it.type == type }
        .filter { status == null || it.status == status }
        .sortedByDescending { it.createdAt }
        .drop(offset.toInt())
        .take(limit)

    override suspend fun getById(id: String): Report? = stored[id]

    override suspend fun getByAuthor(authorId: String): List<Report> =
        stored.values.filter { it.authorId == authorId }

    override suspend fun create(report: Report): Report {
        stored[report.id] = report
        return report
    }

    override suspend fun updateStatus(id: String, status: ReportStatus): Report? {
        val updated = stored[id]?.copy(status = status, updatedAt = Instant.now()) ?: return null
        stored[id] = updated
        return updated
    }

    override suspend fun update(report: Report): Report? {
        if (!stored.containsKey(report.id)) return null
        stored[report.id] = report
        return report
    }

    override suspend fun delete(id: String): Boolean = stored.remove(id) != null
}

class FakeAchievementsRepository : AchievementsRepository {
    val stored = mutableMapOf<String, MutableList<Achievement>>()

    override suspend fun getByUser(userId: String): List<Achievement> = stored[userId].orEmpty()

    override suspend fun saveAll(achievements: List<Achievement>) {
        achievements.forEach { achievement ->
            val forUser = stored.getOrPut(achievement.userId) { mutableListOf() }
            forUser.removeAll { it.code == achievement.code }
            forUser += achievement
        }
    }
}

class RecordingUploadsRepository(owners: Map<String, String> = emptyMap()) : UploadsRepository {
    val stored = owners.toMutableMap()

    override suspend fun record(name: String, userId: String) {
        stored[name] = userId
    }

    override suspend fun ownerOf(name: String): String? = stored[name]

    override suspend fun delete(name: String) {
        stored.remove(name)
    }
}
