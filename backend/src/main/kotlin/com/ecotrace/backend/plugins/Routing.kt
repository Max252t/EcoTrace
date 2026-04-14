package com.ecotrace.backend.plugins

import com.ecotrace.backend.auth.JwtConfig
import com.ecotrace.backend.domain.repository.ReportsRepository
import com.ecotrace.backend.domain.repository.UsersRepository
import com.ecotrace.backend.routes.authRoutes
import com.ecotrace.backend.routes.reportsRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(
    reportsRepository: ReportsRepository,
    usersRepository: UsersRepository,
    jwtConfig: JwtConfig,
) {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok", "service" to "EcoTrace API"))
        }

        authRoutes(usersRepository, jwtConfig)
        reportsRoutes(reportsRepository)
    }
}
