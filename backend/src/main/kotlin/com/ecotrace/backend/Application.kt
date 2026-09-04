package com.ecotrace.backend

import com.ecotrace.backend.auth.JwtConfig
import com.ecotrace.backend.data.db.DatabaseFactory
import com.ecotrace.backend.data.repository.AchievementsRepositoryImpl
import com.ecotrace.backend.data.repository.ReportsRepositoryImpl
import com.ecotrace.backend.data.repository.UsersRepositoryImpl
import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.plugins.configureRouting
import com.ecotrace.backend.plugins.configureSecurity
import com.ecotrace.backend.plugins.configureSerialization
import com.ecotrace.backend.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import java.io.File

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init(this)

    val reportsRepository = ReportsRepositoryImpl()
    val usersRepository = UsersRepositoryImpl()
    val achievementsRepository = AchievementsRepositoryImpl()

    val jwtConfig = JwtConfig(this)

    val storageConfig = environment.config.config("storage")
    val fileStorage = FileStorage(
        directory = File(storageConfig.property("uploadDir").getString()),
        maxFileSizeBytes = storageConfig.property("maxFileSizeBytes").getString().toLong(),
    )

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    install(CallLogging)

    configureSerialization()
    configureSecurity(jwtConfig)
    configureStatusPages()
    configureRouting(
        reportsRepository,
        usersRepository,
        achievementsRepository,
        fileStorage,
        jwtConfig,
    )
}
