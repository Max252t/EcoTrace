package com.topit.ecotrace.ui

import androidx.compose.runtime.compositionLocalOf

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage { RU, EN }

val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }
val LocalOnThemeChange = compositionLocalOf<(ThemeMode) -> Unit> { {} }
val LocalLanguage = compositionLocalOf { AppLanguage.RU }
val LocalOnLanguageChange = compositionLocalOf<(AppLanguage) -> Unit> { {} }
