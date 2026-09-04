package com.topit.ecotrace.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.topit.ecotrace.domain.model.AchievementCode
import com.topit.ecotrace.presentation.viewmodel.ProfileUiState
import com.topit.ecotrace.presentation.viewmodel.ProfileViewModel
import com.topit.ecotrace.presentation.viewmodel.daggerViewModel
import com.topit.ecotrace.ui.AppStrings
import com.topit.ecotrace.ui.LocalAppStrings

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onSettingsClick: () -> Unit,
) {
    val viewModel: ProfileViewModel = daggerViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        contentPadding = contentPadding,
        onSettingsClick = onSettingsClick,
        state = state,
    )
}

@Composable
internal fun ProfileContent(
    contentPadding: PaddingValues,
    onSettingsClick: () -> Unit,
    state: ProfileUiState,
) {
    val s = LocalAppStrings.current
    val unlockedCodes = state.achievements.map { it.code }.toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Hero gradient header ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            state.displayName.ifBlank { s.resident },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            s.ecoVolunteer(state.stats.level),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                }

                // XP progress
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                s.toNextLevel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            )
                            Text(
                                s.levelProgress(state.stats.ecoPoints, state.stats.pointsForNextLevel),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { state.stats.levelProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.onPrimary,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                            strokeCap = StrokeCap.Round,
                        )
                    }
                }
            }

            // Settings button — top-right of hero
            FilledIconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = s.settingsTitle,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ── Body ─────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Stat cards
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatCard(
                    state.stats.reportsSubmitted.toString(),
                    s.reportsSubmitted,
                    Icons.Default.Report,
                    Modifier.weight(1f),
                )
                StatCard(
                    state.stats.problemsSolved.toString(),
                    s.problemsSolved,
                    Icons.Default.CheckCircle,
                    Modifier.weight(1f),
                )
                StatCard(
                    state.stats.ecoPoints.toString(),
                    s.ecoPoints,
                    Icons.Default.Star,
                    Modifier.weight(1f),
                )
            }

            EcoSection(title = s.achievements) {
                Text(
                    s.achievementsProgress(unlockedCodes.size, AchievementCode.entries.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                AchievementCode.entries.forEach { code ->
                    AchievementRow(
                        icon = code.icon(),
                        title = code.title(s),
                        subtitle = code.subtitle(s),
                        unlocked = code in unlockedCodes,
                    )
                }
            }
        }
    }
}

private fun AchievementCode.icon(): ImageVector = when (this) {
    AchievementCode.FIRST_REPORT -> Icons.Default.EmojiEvents
    AchievementCode.REPORTER_10 -> Icons.Default.Report
    AchievementCode.FOREST_DEFENDER -> Icons.Default.Forest
    AchievementCode.PROBLEM_SOLVER -> Icons.Default.CheckCircle
    AchievementCode.LEVEL_5 -> Icons.Default.Star
}

private fun AchievementCode.title(s: AppStrings): String = when (this) {
    AchievementCode.FIRST_REPORT -> s.firstReport
    AchievementCode.REPORTER_10 -> s.reporter10
    AchievementCode.FOREST_DEFENDER -> s.forestDefender
    AchievementCode.PROBLEM_SOLVER -> s.problemSolver
    AchievementCode.LEVEL_5 -> s.level5
}

private fun AchievementCode.subtitle(s: AppStrings): String = when (this) {
    AchievementCode.FIRST_REPORT -> s.firstReportSub
    AchievementCode.REPORTER_10 -> s.reporter10Sub
    AchievementCode.FOREST_DEFENDER -> s.forestDefenderSub
    AchievementCode.PROBLEM_SOLVER -> s.problemSolverSub
    AchievementCode.LEVEL_5 -> s.level5Sub
}

@Composable
private fun StatCard(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AchievementRow(icon: ImageVector, title: String, subtitle: String, unlocked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (unlocked) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
