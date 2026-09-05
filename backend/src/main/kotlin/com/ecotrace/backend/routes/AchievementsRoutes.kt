package com.ecotrace.backend.routes

import com.ecotrace.backend.domain.model.Achievement
import com.ecotrace.backend.domain.model.AchievementCode
import com.ecotrace.backend.domain.model.AchievementRules
import com.ecotrace.backend.domain.model.SyncAchievementsRequest
import com.ecotrace.backend.domain.model.toResponse
import com.ecotrace.backend.domain.repository.AchievementsRepository
import com.ecotrace.backend.domain.repository.ReportsRepository
import com.ecotrace.backend.domain.repository.UsersRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant

fun Route.achievementsRoutes(
    achievementsRepository: AchievementsRepository,
    reportsRepository: ReportsRepository,
    usersRepository: UsersRepository,
) {
    authenticate("auth-jwt") {
        route("/api/achievements") {

            get {
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val achievements = achievementsRepository.getByUser(userId)
                call.respond(achievements.map { it.toResponse() })
            }

            post("/sync") {
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<SyncAchievementsRequest>()
                val claimed = mutableListOf<Achievement>()

                for (dto in request.achievements) {
                    val code = runCatching { AchievementCode.valueOf(dto.code) }.getOrNull()
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Unknown achievement: ${dto.code}"),
                        )
                    val unlockedAt = runCatching { Instant.parse(dto.unlockedAt) }.getOrNull()
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid unlockedAt: ${dto.unlockedAt}"),
                        )
                    claimed += Achievement(userId = userId, code = code, unlockedAt = unlockedAt)
                }

                val stored = achievementsRepository.getByUser(userId)
                val eligible = AchievementRules.eligible(reportsRepository.getByAuthor(userId))
                val merged = AchievementRules.merge(
                    userId = userId,
                    stored = stored,
                    claimed = claimed,
                    eligible = eligible,
                    now = Instant.now(),
                    notBefore = usersRepository.findById(userId)?.createdAt ?: Instant.EPOCH,
                )

                val changed = merged.filter { achievement ->
                    stored.none { it.code == achievement.code && it.unlockedAt == achievement.unlockedAt }
                }
                if (changed.isNotEmpty()) {
                    achievementsRepository.saveAll(changed)
                }

                call.respond(merged.map { it.toResponse() })
            }
        }
    }
}
