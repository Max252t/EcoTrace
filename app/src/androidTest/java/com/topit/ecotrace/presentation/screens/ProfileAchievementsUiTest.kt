package com.topit.ecotrace.presentation.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.AchievementCode
import com.topit.ecotrace.domain.model.UserStats
import com.topit.ecotrace.presentation.viewmodel.ProfileUiState
import com.topit.ecotrace.ui.AppLanguage
import com.topit.ecotrace.ui.LocalAppStrings
import com.topit.ecotrace.ui.appStringsFor
import com.topit.ecotrace.ui.theme.EcoTraceTheme
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ProfileAchievementsUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun profile_showsUnlockedAndLockedAchievements() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val strings = appStringsFor(context, AppLanguage.RU)

        composeRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides strings) {
                EcoTraceTheme(darkTheme = false, dynamicColor = false) {
                    ProfileContent(
                        contentPadding = PaddingValues(),
                        onSettingsClick = {},
                        state = ProfileUiState(
                            displayName = "Иван",
                            stats = UserStats(reportsSubmitted = 3, problemsSolved = 1, ecoPoints = 90),
                            achievements = listOf(
                                Achievement(
                                    code = AchievementCode.FIRST_REPORT,
                                    unlockedAt = Instant.parse("2026-05-01T10:00:00Z"),
                                    synced = true,
                                ),
                            ),
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Иван").assertIsDisplayed()
        composeRule.onNodeWithText(strings.ecoVolunteer(1)).assertIsDisplayed()
        composeRule.onNodeWithText(strings.levelProgress(90, 100)).assertIsDisplayed()
        composeRule.onNodeWithText(strings.achievements).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(strings.achievementsProgress(1, AchievementCode.entries.size))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(strings.firstReport).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(strings.problemSolver).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(strings.level5).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun profile_showsStatsCalculatedFromReports() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val strings = appStringsFor(context, AppLanguage.RU)

        composeRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides strings) {
                EcoTraceTheme(darkTheme = false, dynamicColor = false) {
                    ProfileContent(
                        contentPadding = PaddingValues(),
                        onSettingsClick = {},
                        state = ProfileUiState(
                            stats = UserStats(reportsSubmitted = 12, problemsSolved = 8, ecoPoints = 480),
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithText("12").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("8").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("480").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(strings.resident).assertIsDisplayed()
    }
}
