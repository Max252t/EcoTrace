package com.ecotrace.backend.routes

import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.data.storage.StoredFile
import com.ecotrace.backend.domain.model.UploadedFileResponse
import com.ecotrace.backend.domain.repository.UploadsRepository
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.filesRoutes(fileStorage: FileStorage, uploadsRepository: UploadsRepository) {
    authenticate("auth-jwt") {
        post(FileStorage.ROUTE) {
            val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            var stored: StoredFile? = null

            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem && stored == null) {
                    stored = withContext(Dispatchers.IO) {
                        part.provider().toInputStream().use { fileStorage.save(it) }
                    }
                }
                part.dispose()
            }

            when (val result = stored) {
                null -> call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "File part is missing"),
                )

                is StoredFile.Success -> {
                    uploadsRepository.record(result.name, userId)
                    call.respond(
                        HttpStatusCode.Created,
                        UploadedFileResponse(name = result.name, url = result.url),
                    )
                }

                StoredFile.TooLarge -> call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    mapOf("error" to "File is too large"),
                )

                StoredFile.UnsupportedType -> call.respond(
                    HttpStatusCode.UnsupportedMediaType,
                    mapOf("error" to "Only JPEG, PNG and WebP images are supported"),
                )
            }
        }
    }
}
