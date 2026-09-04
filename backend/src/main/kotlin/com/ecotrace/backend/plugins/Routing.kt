package com.ecotrace.backend.plugins

import com.ecotrace.backend.auth.JwtConfig
import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.domain.repository.AchievementsRepository
import com.ecotrace.backend.domain.repository.ReportsRepository
import com.ecotrace.backend.domain.repository.UsersRepository
import com.ecotrace.backend.routes.achievementsRoutes
import com.ecotrace.backend.routes.authRoutes
import com.ecotrace.backend.routes.filesRoutes
import com.ecotrace.backend.routes.reportsRoutes
import com.ecotrace.backend.routes.usersRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(
    reportsRepository: ReportsRepository,
    usersRepository: UsersRepository,
    achievementsRepository: AchievementsRepository,
    fileStorage: FileStorage,
    jwtConfig: JwtConfig,
) {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok", "service" to "EcoTrace API"))
        }

        authRoutes(usersRepository, jwtConfig)
        reportsRoutes(reportsRepository, fileStorage)
        achievementsRoutes(achievementsRepository, reportsRepository)
        usersRoutes(usersRepository)
        filesRoutes(fileStorage)
        staticFiles(FileStorage.ROUTE, fileStorage.directory)
    }
}
