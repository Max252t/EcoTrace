package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.mapper.toDomain
import com.topit.ecotrace.data.remote.api.AuthApi
import com.topit.ecotrace.data.remote.api.LoginRequestDto
import com.topit.ecotrace.data.remote.api.RegisterRequestDto
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import javax.inject.Inject
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
        }.mapErrorMessage()
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
        }.mapErrorMessage()
    }

    private fun Result<AuthSession>.mapErrorMessage(): Result<AuthSession> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                val mapped = when (error) {
                    is HttpException -> when (error.code()) {
                        401 -> "Неверный email или пароль"
                        409 -> "Этот email уже зарегистрирован"
                        400 -> "Проверьте корректность введенных данных"
                        else -> "Ошибка сервера: ${error.code()}"
                    }
                    else -> "Не удалось подключиться к серверу. Проверьте IP/порт backend"
                }
                Result.failure(IllegalStateException(mapped))
            },
        )
    }

    override fun currentSession(): AuthSession? = sessionStorage.read()

    override fun logout() = sessionStorage.clear()
}
