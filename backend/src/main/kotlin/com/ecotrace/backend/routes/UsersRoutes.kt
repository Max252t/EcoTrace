package com.ecotrace.backend.routes

import com.ecotrace.backend.domain.model.toPublicResponse
import com.ecotrace.backend.domain.repository.UsersRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.usersRoutes(usersRepository: UsersRepository) {
    authenticate("auth-jwt") {
        route("/api/users") {

            get("{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                val user = usersRepository.findById(id)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "User not found"),
                    )

                call.respond(user.toPublicResponse())
            }
        }
    }
}
