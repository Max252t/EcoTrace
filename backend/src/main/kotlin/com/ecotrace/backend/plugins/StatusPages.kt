package com.ecotrace.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            call.application.environment.log.debug("Malformed request body", cause)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Malformed request body"))
        }
        exception<BadRequestException> { call, cause ->
            call.application.environment.log.debug("Bad request", cause)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Malformed request"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.application.environment.log.debug("Invalid request", cause)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
        }
    }
}
