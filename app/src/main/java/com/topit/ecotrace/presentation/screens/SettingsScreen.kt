package com.topit.ecotrace.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.topit.ecotrace.ui.AppLanguage
import com.topit.ecotrace.ui.LocalAppStrings
import com.topit.ecotrace.ui.LocalLanguage
import com.topit.ecotrace.ui.LocalOnLanguageChange
import com.topit.ecotrace.ui.LocalOnThemeChange
import com.topit.ecotrace.ui.LocalThemeMode
import com.topit.ecotrace.ui.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    val s = LocalAppStrings.current
    val themeMode = LocalThemeMode.current
    val onThemeChange = LocalOnThemeChange.current
    val language = LocalLanguage.current
    val onLanguageChange = LocalOnLanguageChange.current

    AdaptiveContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(
                icon = Icons.Default.Settings,
                title = s.settingsTitle,
                subtitle = s.themeTitle + " · " + s.languageTitle,
                onBack = onBack,
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── Theme ────────────────────────────────────────────────────
                EcoSection(title = s.themeTitle) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            s.themeTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.values().forEachIndexed { index, mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> s.themeSystem
                                ThemeMode.LIGHT  -> s.themeLight
                                ThemeMode.DARK   -> s.themeDark
                            }
                            val icon: ImageVector = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Default.Settings
                                ThemeMode.LIGHT  -> Icons.Default.LightMode
                                ThemeMode.DARK   -> Icons.Default.DarkMode
                            }
                            // icon passed as named param so it sits in its own slot,
                            // trailing lambda = label — avoids icon/text overlap
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeMode.values().size,
                                ),
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // ── Language ─────────────────────────────────────────────────
                EcoSection(title = s.languageTitle) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            s.languageTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AppLanguage.values().forEachIndexed { index, lang ->
                            val label = if (lang == AppLanguage.RU) s.langRussian else s.langEnglish
                            SegmentedButton(
                                selected = language == lang,
                                onClick = { onLanguageChange(lang) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = AppLanguage.values().size,
                                ),
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledIconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = s.logoutButton,
                        )
                    }
                }
            }
        }
    }
}
