package com.ecotrace.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import java.util.Date

class JwtConfig(application: Application) {
    private val config = application.environment.config.config("jwt")

    val secret: String = config.property("secret").getString()
    val issuer: String = config.property("issuer").getString()
    val audience: String = config.property("audience").getString()
    val realm: String = config.property("realm").getString()
    private val expiresInMs: Long = config.property("expiresInMs").getString().toLong()

    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(userId: String, email: String, role: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withClaim("email", email)
        .withClaim("role", role)
        .withExpiresAt(Date(System.currentTimeMillis() + expiresInMs))
        .sign(algorithm)

    fun verifier() = JWT.require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
}
