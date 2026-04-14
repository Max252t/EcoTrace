package com.ecotrace.backend.routes

import com.ecotrace.backend.domain.model.CreateReportRequest
import com.ecotrace.backend.domain.model.ProblemType
import com.ecotrace.backend.domain.model.Report
import com.ecotrace.backend.domain.model.ReportStatus
import com.ecotrace.backend.domain.model.UpdateStatusRequest
import com.ecotrace.backend.domain.model.toResponse
import com.ecotrace.backend.domain.repository.ReportsRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.util.UUID

fun Route.reportsRoutes(reportsRepository: ReportsRepository) {
    route("/api/reports") {

        // GET /api/reports?type=DUMP&status=OPEN  — публичный
        get {
            val type = call.request.queryParameters["type"]
                ?.let { runCatching { ProblemType.valueOf(it) }.getOrNull() }
            val status = call.request.queryParameters["status"]
                ?.let { runCatching { ReportStatus.valueOf(it) }.getOrNull() }

            val reports = reportsRepository.getAll(type, status)
            call.respond(reports.map { it.toResponse() })
        }

        // GET /api/reports/{id}  — публичный
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val report = reportsRepository.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Report not found"))
            call.respond(report.toResponse())
        }

        // Защищённые роуты
        authenticate("auth-jwt") {

            // POST /api/reports
            post {
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreateReportRequest>()

                val type = runCatching { ProblemType.valueOf(request.type) }.getOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Unknown type: ${request.type}"),
                    )

                val now = Instant.now()
                val report = Report(
                    id = UUID.randomUUID().toString(),
                    title = request.title.trim(),
                    description = request.description.trim(),
                    type = type,
                    status = ReportStatus.OPEN,
                    latitude = request.latitude,
                    longitude = request.longitude,
                    imageUrl = request.imageUrl,
                    authorId = userId,
                    createdAt = now,
                    updatedAt = now,
                )
                val created = reportsRepository.create(report)
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            // PATCH /api/reports/{id}/status
            patch("{id}/status") {
                val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val role = call.principal<JWTPrincipal>()?.getClaim("role", String::class) ?: ""

                val existing = reportsRepository.getById(id)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))

                // только автор или ADMIN могут менять статус
                if (existing.authorId != userId && role != "ADMIN") {
                    return@patch call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                val request = call.receive<UpdateStatusRequest>()
                val newStatus = runCatching { ReportStatus.valueOf(request.status) }.getOrNull()
                    ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Unknown status: ${request.status}"),
                    )

                val updated = reportsRepository.updateStatus(id, newStatus)
                    ?: return@patch call.respond(HttpStatusCode.NotFound)
                call.respond(updated.toResponse())
            }

            // DELETE /api/reports/{id}
            delete("{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val role = call.principal<JWTPrincipal>()?.getClaim("role", String::class) ?: ""

                val existing = reportsRepository.getById(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))

                if (existing.authorId != userId && role != "ADMIN") {
                    return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                reportsRepository.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    // GET /api/users/{userId}/reports
    authenticate("auth-jwt") {
        get("/api/users/me/reports") {
            val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val reports = reportsRepository.getByAuthor(userId)
            call.respond(reports.map { it.toResponse() })
        }
    }
}
