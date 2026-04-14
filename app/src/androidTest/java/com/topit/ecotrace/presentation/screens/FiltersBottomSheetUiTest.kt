package com.topit.ecotrace.presentation.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.ui.AppLanguage
import com.topit.ecotrace.ui.LocalAppStrings
import com.topit.ecotrace.ui.appStringsFor
import com.topit.ecotrace.ui.theme.EcoTraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FiltersBottomSheetUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun resolvedStatus_isVisible_andCanBeApplied() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val strings = appStringsFor(context, AppLanguage.RU)
        var appliedStatuses: Set<ReportStatus> = emptySet()

        composeRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides strings) {
                EcoTraceTheme(darkTheme = false, dynamicColor = false) {
                    FiltersBottomSheet(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                        onApply = { _, statuses -> appliedStatuses = statuses },
                    )
                }
            }
        }

        composeRule.onNodeWithText(strings.statusResolved).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(strings.applyFilters).performClick()

        assertTrue(appliedStatuses.contains(ReportStatus.RESOLVED))
    }

    @Test
    fun resetAll_clearsInitiallySelectedFilters() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val strings = appStringsFor(context, AppLanguage.RU)
        var applyCalls = 0
        var lastTypes: Set<ProblemType> = emptySet()
        var lastStatuses: Set<ReportStatus> = emptySet()

        composeRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides strings) {
                EcoTraceTheme(darkTheme = false, dynamicColor = false) {
                    FiltersBottomSheet(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                        initialTypes = setOf(ProblemType.DUMP),
                        initialStatuses = setOf(ReportStatus.OPEN),
                        onApply = { types, statuses ->
                            applyCalls += 1
                            lastTypes = types
                            lastStatuses = statuses
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText(strings.resetAll).assertIsDisplayed().performClick()

        assertTrue(applyCalls >= 1)
        assertEquals(emptySet<ProblemType>(), lastTypes)
        assertEquals(emptySet<ReportStatus>(), lastStatuses)
    }
}
