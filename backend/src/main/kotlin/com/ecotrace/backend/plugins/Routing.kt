package com.ecotrace.backend.plugins

import com.ecotrace.backend.auth.JwtConfig
import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.domain.repository.AchievementsRepository
import com.ecotrace.backend.domain.repository.ReportsRepository
import com.ecotrace.backend.domain.repository.UploadsRepository
import com.ecotrace.backend.domain.repository.UsersRepository
import com.ecotrace.backend.routes.achievementsRoutes
import com.ecotrace.backend.routes.authRoutes
import com.ecotrace.backend.routes.filesRoutes
import com.ecotrace.backend.routes.reportsRoutes
import com.ecotrace.backend.routes.usersRoutes
import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val STATIC_CACHE_SECONDS = 86_400

fun Application.configureRouting(
    reportsRepository: ReportsRepository,
    usersRepository: UsersRepository,
    achievementsRepository: AchievementsRepository,
    uploadsRepository: UploadsRepository,
    fileStorage: FileStorage,
    jwtConfig: JwtConfig,
) {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok", "service" to "EcoTrace API"))
        }

        authRoutes(usersRepository, jwtConfig)
        reportsRoutes(reportsRepository, uploadsRepository, fileStorage)
        achievementsRoutes(achievementsRepository, reportsRepository, usersRepository)
        usersRoutes(usersRepository)
        filesRoutes(fileStorage, uploadsRepository)
        staticFiles(FileStorage.ROUTE, fileStorage.directory) {
            cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = STATIC_CACHE_SECONDS)) }
            modify { _, call -> call.response.headers.append("X-Content-Type-Options", "nosniff") }
        }
    }
}
