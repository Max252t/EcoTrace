package com.topit.ecotrace.presentation.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.ui.AppLanguage
import com.topit.ecotrace.ui.LocalAppStrings
import com.topit.ecotrace.ui.appStringsFor
import com.topit.ecotrace.ui.theme.EcoTraceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ReportDetailsUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun statusPicker_movesReportToInProgress() {
        val strings = appStringsFor(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppLanguage.RU,
        )
        var picked: ReportStatus? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides strings) {
                EcoTraceTheme(darkTheme = false, dynamicColor = false) {
                    ReportDetailsContent(
                        contentPadding = PaddingValues(),
                        onBack = {},
                        report = openReport(),
                        isLoading = false,
                        authorName = "Иван",
                        availableStatuses = listOf(ReportStatus.OPEN, ReportStatus.IN_PROGRESS),
                        onStatusChange = { picked = it },
                    )
                }
            }
        }

        composeRule.onNodeWithText(strings.statusInProgress)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals(ReportStatus.IN_PROGRESS, picked)
    }

    @Test
    fun statusPicker_isHiddenForUsersWhoMayNotChangeStatus() {
        val strings = appStringsFor(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppLanguage.RU,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides strings) {
                EcoTraceTheme(darkTheme = false, dynamicColor = false) {
                    ReportDetailsContent(
                        contentPadding = PaddingValues(),
                        onBack = {},
                        report = openReport(),
                        isLoading = false,
                        authorName = "Мария",
                        availableStatuses = emptyList(),
                        onStatusChange = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(strings.statusInProgress).assertDoesNotExist()
        composeRule.onNodeWithText(strings.statusResolved).assertDoesNotExist()
    }

    private fun openReport() = Report(
        id = "report-1",
        title = "title",
        description = "description",
        type = ProblemType.DUMP,
        status = ReportStatus.OPEN,
        latitude = 55.0,
        longitude = 37.0,
        authorId = "user-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = true,
    )
}
