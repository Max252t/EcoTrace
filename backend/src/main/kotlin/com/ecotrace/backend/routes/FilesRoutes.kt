package com.ecotrace.backend.routes

import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.data.storage.StoredFile
import com.ecotrace.backend.domain.model.UploadedFileResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

fun Route.filesRoutes(fileStorage: FileStorage) {
    authenticate("auth-jwt") {
        post(FileStorage.ROUTE) {
            var bytes: ByteArray? = null
            var contentType: String? = null
            var originalFileName: String? = null

            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem && bytes == null) {
                    bytes = part.provider().readRemaining().readByteArray()
                    contentType = part.contentType?.toString()
                    originalFileName = part.originalFileName
                }
                part.dispose()
            }

            val content = bytes
            if (content == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "File part is missing"))
                return@post
            }

            when (val stored = fileStorage.save(content, contentType, originalFileName)) {
                is StoredFile.Success -> call.respond(
                    HttpStatusCode.Created,
                    UploadedFileResponse(name = stored.name, url = stored.url),
                )

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
