package com.ecotrace.backend.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.ecotrace.backend.auth.JwtConfig
import com.ecotrace.backend.domain.model.AuthResponse
import com.ecotrace.backend.domain.model.LoginRequest
import com.ecotrace.backend.domain.model.RegisterRequest
import com.ecotrace.backend.domain.model.User
import com.ecotrace.backend.domain.model.UserRole
import com.ecotrace.backend.domain.repository.UsersRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.util.UUID

fun Route.authRoutes(
    usersRepository: UsersRepository,
    jwtConfig: JwtConfig,
) {
    route("/api/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()

            if (request.email.isBlank() || request.password.length < 6) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid email or password (min 6 chars)"))
                return@post
            }

            if (usersRepository.existsByEmail(request.email)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
                return@post
            }

            val hash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
            val user = User(
                id = UUID.randomUUID().toString(),
                email = request.email.lowercase().trim(),
                passwordHash = hash,
                displayName = request.displayName.trim(),
                role = UserRole.USER,
                createdAt = Instant.now(),
            )
            usersRepository.create(user)

            val token = jwtConfig.generateToken(user.id, user.email, user.role.name)
            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    token = token,
                    userId = user.id,
                    email = user.email,
                    displayName = user.displayName,
                    role = user.role.name,
                ),
            )
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = usersRepository.findByEmail(request.email.lowercase().trim())
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

            val passwordMatch = BCrypt.verifyer()
                .verify(request.password.toCharArray(), user.passwordHash)
                .verified

            if (!passwordMatch) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

            val token = jwtConfig.generateToken(user.id, user.email, user.role.name)
            call.respond(
                AuthResponse(
                    token = token,
                    userId = user.id,
                    email = user.email,
                    displayName = user.displayName,
                    role = user.role.name,
                ),
            )
        }
    }
}
