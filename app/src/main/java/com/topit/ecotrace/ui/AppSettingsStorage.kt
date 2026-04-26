package com.topit.ecotrace.ui

import android.content.Context

class AppSettingsStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name).orEmpty()
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getLanguage(): AppLanguage {
        val raw = prefs.getString(KEY_LANGUAGE, AppLanguage.RU.name).orEmpty()
        return runCatching { AppLanguage.valueOf(raw) }.getOrDefault(AppLanguage.RU)
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "ecotrace_app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LANGUAGE = "language"
    }
}
