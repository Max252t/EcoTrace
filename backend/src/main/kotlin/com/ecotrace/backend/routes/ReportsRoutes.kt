package com.ecotrace.backend.routes

import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.domain.model.CreateReportRequest
import com.ecotrace.backend.domain.model.Limits
import com.ecotrace.backend.domain.model.ProblemType
import com.ecotrace.backend.domain.model.Report
import com.ecotrace.backend.domain.model.ReportStatus
import com.ecotrace.backend.domain.model.UpdateStatusRequest
import com.ecotrace.backend.domain.model.toResponse
import com.ecotrace.backend.domain.repository.ReportsRepository
import com.ecotrace.backend.domain.repository.UploadsRepository
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

private const val ADMIN_ROLE = "ADMIN"

private val AUTHOR_STATUSES = setOf(ReportStatus.OPEN, ReportStatus.IN_PROGRESS)

fun Route.reportsRoutes(
    reportsRepository: ReportsRepository,
    uploadsRepository: UploadsRepository,
    fileStorage: FileStorage,
) {
    route("/api/reports") {

        get {
            val type = call.request.queryParameters["type"]
                ?.let { runCatching { ProblemType.valueOf(it) }.getOrNull() }
            val status = call.request.queryParameters["status"]
                ?.let { runCatching { ReportStatus.valueOf(it) }.getOrNull() }

            val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                ?.coerceIn(1, Limits.PAGE_SIZE_MAX)
                ?: Limits.PAGE_SIZE_DEFAULT
            val offset = call.request.queryParameters["offset"]?.toLongOrNull()?.coerceAtLeast(0) ?: 0

            val reports = reportsRepository.getAll(type, status, limit, offset)
            call.respond(reports.map { it.toResponse() })
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val report = reportsRepository.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Report not found"))
            call.respond(report.toResponse())
        }

        authenticate("auth-jwt") {

            post {
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreateReportRequest>()

                val type = runCatching { ProblemType.valueOf(request.type) }.getOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Unknown type: ${request.type}"),
                    )

                val title = request.title.trim()
                val description = request.description.trim()

                if (title.isEmpty() || title.length > Limits.TITLE_MAX) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Title must be 1..${Limits.TITLE_MAX} characters"),
                    )
                }
                if (description.length > Limits.DESCRIPTION_MAX) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Description is too long"),
                    )
                }
                if (!Limits.isLatitude(request.latitude) || !Limits.isLongitude(request.longitude)) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Coordinates are out of range"),
                    )
                }

                val requestedImage = request.imageUrl?.trim()?.takeIf { it.isNotEmpty() }
                var imageUrl: String? = null
                if (requestedImage != null) {
                    val name = FileStorage.nameFromUrl(requestedImage)
                        ?: FileStorage.nameFromUrl(FileStorage.urlFor(requestedImage))
                    if (name == null || uploadsRepository.ownerOf(name) != userId) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Unknown image; upload it with POST /api/files first"),
                        )
                    }
                    imageUrl = FileStorage.urlFor(name)
                }

                val now = Instant.now()
                val report = Report(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    type = type,
                    status = ReportStatus.OPEN,
                    latitude = request.latitude,
                    longitude = request.longitude,
                    imageUrl = imageUrl,
                    authorId = userId,
                    createdAt = now,
                    updatedAt = now,
                )
                val created = reportsRepository.create(report)
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            patch("{id}/status") {
                val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val role = call.principal<JWTPrincipal>()?.getClaim("role", String::class) ?: ""

                val existing = reportsRepository.getById(id)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))

                val isAdmin = role == ADMIN_ROLE
                if (existing.authorId != userId && !isAdmin) {
                    return@patch call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                val request = call.receive<UpdateStatusRequest>()
                val newStatus = runCatching { ReportStatus.valueOf(request.status) }.getOrNull()
                    ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Unknown status: ${request.status}"),
                    )

                if (!isAdmin && newStatus !in AUTHOR_STATUSES) {
                    return@patch call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only an administrator can resolve a report"),
                    )
                }

                val updated = reportsRepository.updateStatus(id, newStatus)
                    ?: return@patch call.respond(HttpStatusCode.NotFound)
                call.respond(updated.toResponse())
            }

            delete("{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val role = call.principal<JWTPrincipal>()?.getClaim("role", String::class) ?: ""

                val existing = reportsRepository.getById(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))

                if (existing.authorId != userId && role != ADMIN_ROLE) {
                    return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                reportsRepository.delete(id)
                FileStorage.nameFromUrl(existing.imageUrl)?.let { name ->
                    fileStorage.delete(existing.imageUrl)
                    uploadsRepository.delete(name)
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    authenticate("auth-jwt") {
        get("/api/users/me/reports") {
            val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val reports = reportsRepository.getByAuthor(userId)
            call.respond(reports.map { it.toResponse() })
        }
    }
}
