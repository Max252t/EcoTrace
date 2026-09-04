package com.topit.ecotrace.data.local

import android.content.Context
import com.topit.ecotrace.domain.repository.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStorage @Inject constructor(context: Context) {
    private val prefs = context.getSharedPreferences("ecotrace_session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(readFromPrefs())
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    fun save(session: AuthSession) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_ROLE, session.role)
            .apply()
        _session.value = session
    }

    fun read(): AuthSession? = _session.value

    fun token(): String? = _session.value?.token

    fun userId(): String? = _session.value?.userId

    fun clear() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private fun readFromPrefs(): AuthSession? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        return AuthSession(
            token = token,
            userId = userId,
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            role = prefs.getString(KEY_ROLE, "USER").orEmpty(),
        )
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_ROLE = "role"
    }
}
