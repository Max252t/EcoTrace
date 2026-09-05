package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.mapper.toDomain
import com.topit.ecotrace.data.remote.api.AuthApi
import com.topit.ecotrace.data.remote.api.LoginRequestDto
import com.topit.ecotrace.data.remote.api.RegisterRequestDto
import com.topit.ecotrace.domain.repository.AuthError
import com.topit.ecotrace.domain.repository.AuthFailure
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class BackendAuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStorage: SessionStorage,
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<AuthSession> {
        return runCatching {
            val session = authApi.login(LoginRequestDto(email.trim(), password)).toDomain()
            sessionStorage.save(session)
            session
        }.mapError()
    }

    override suspend fun register(name: String, email: String, password: String): Result<AuthSession> {
        return runCatching {
            val session = authApi.register(
                RegisterRequestDto(
                    email = email.trim(),
                    password = password,
                    displayName = name.trim(),
                ),
            ).toDomain()
            sessionStorage.save(session)
            session
        }.mapError()
    }

    private fun Result<AuthSession>.mapError(): Result<AuthSession> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                val mapped = when (error) {
                    is HttpException -> when (error.code()) {
                        HTTP_UNAUTHORIZED -> AuthError.INVALID_CREDENTIALS
                        HTTP_CONFLICT -> AuthError.EMAIL_TAKEN
                        HTTP_BAD_REQUEST -> AuthError.INVALID_DATA
                        HTTP_TOO_MANY_REQUESTS -> AuthError.TOO_MANY_ATTEMPTS
                        else -> AuthError.SERVER
                    }

                    else -> AuthError.NETWORK
                }
                Result.failure(AuthFailure(mapped))
            },
        )
    }

    override fun currentSession(): AuthSession? = sessionStorage.read()

    override fun observeSession(): Flow<AuthSession?> = sessionStorage.session

    override fun logout() = sessionStorage.clear()

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_CONFLICT = 409
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
