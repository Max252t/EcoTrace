package com.ecotrace.backend

import com.ecotrace.backend.auth.JwtConfig
import com.ecotrace.backend.data.db.DatabaseFactory
import com.ecotrace.backend.data.repository.AchievementsRepositoryImpl
import com.ecotrace.backend.data.repository.ReportsRepositoryImpl
import com.ecotrace.backend.data.repository.UploadsRepositoryImpl
import com.ecotrace.backend.data.repository.UsersRepositoryImpl
import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.plugins.configureRouting
import com.ecotrace.backend.plugins.configureSecurity
import com.ecotrace.backend.plugins.configureSerialization
import com.ecotrace.backend.plugins.configureStatusPages
import com.ecotrace.backend.routes.AUTH_RATE_LIMIT
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import java.io.File
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init(this)

    val reportsRepository = ReportsRepositoryImpl()
    val usersRepository = UsersRepositoryImpl()
    val achievementsRepository = AchievementsRepositoryImpl()
    val uploadsRepository = UploadsRepositoryImpl()

    val jwtConfig = JwtConfig(this)

    val storageConfig = environment.config.config("storage")
    val fileStorage = FileStorage(
        directory = File(storageConfig.property("uploadDir").getString()),
        maxFileSizeBytes = storageConfig.property("maxFileSizeBytes").getString().toLong(),
    )

    val allowedHosts = environment.config.property("cors.allowedHosts").getString()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (allowedHosts.contains("*")) {
        environment.log.warn("CORS is open to any host; set CORS_ALLOWED_HOSTS for production")
    }

    install(CORS) {
        if (allowedHosts.contains("*")) {
            anyHost()
        } else {
            allowedHosts.forEach { host -> allowHost(host, schemes = listOf("http", "https")) }
        }

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    install(CallLogging)

    install(RateLimit) {
        register(AUTH_RATE_LIMIT) {
            rateLimiter(limit = 10, refillPeriod = 60.seconds)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }

    configureSerialization()
    configureSecurity(jwtConfig)
    configureStatusPages()
    configureRouting(
        reportsRepository,
        usersRepository,
        achievementsRepository,
        uploadsRepository,
        fileStorage,
        jwtConfig,
    )
}
