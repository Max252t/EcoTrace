package com.topit.ecotrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.topit.ecotrace.presentation.EcoTraceAppRoot
import com.topit.ecotrace.ui.AppLanguage
import com.topit.ecotrace.ui.AppSettingsStorage
import com.topit.ecotrace.ui.LocalAppStrings
import com.topit.ecotrace.ui.LocalLanguage
import com.topit.ecotrace.ui.LocalOnLanguageChange
import com.topit.ecotrace.ui.LocalOnThemeChange
import com.topit.ecotrace.ui.LocalThemeMode
import com.topit.ecotrace.ui.ThemeMode
import com.topit.ecotrace.ui.appStringsFor
import com.topit.ecotrace.ui.theme.EcoTraceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsStorage = AppSettingsStorage(applicationContext)
        setContent {
            var themeMode by remember { mutableStateOf(settingsStorage.getThemeMode()) }
            var language by remember { mutableStateOf(settingsStorage.getLanguage()) }

            val strings = remember(language) { appStringsFor(this, language) }
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalThemeMode provides themeMode,
                LocalOnThemeChange provides { mode ->
                    themeMode = mode
                    settingsStorage.setThemeMode(mode)
                },
                LocalLanguage provides language,
                LocalOnLanguageChange provides { lang ->
                    language = lang
                    settingsStorage.setLanguage(lang)
                },
            ) {
                EcoTraceTheme(darkTheme = darkTheme, dynamicColor = false) {
                    EcoTraceAppRoot()
                }
            }
        }
    }
}
