package com.topit.ecotrace.presentation.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.topit.ecotrace.ui.AppLanguage
import com.topit.ecotrace.ui.LocalAppStrings
import com.topit.ecotrace.ui.appStringsFor
import com.topit.ecotrace.ui.theme.EcoTraceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthScreensUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loginScreen_enablesSubmitForValidCredentials_andCallsOnLogin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val strings = appStringsFor(context, AppLanguage.RU)
        var loginEmail: String? = null
        var loginPassword: String? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides strings) {
                EcoTraceTheme(darkTheme = false, dynamicColor = false) {
                    LoginScreen(
                        contentPadding = PaddingValues(),
                        onOpenRegister = {},
                        onLogin = { email, password ->
                            loginEmail = email
                            loginPassword = password
                        },
                    )
                }
            }
        }

        val submitButton = composeRule.onNodeWithText(strings.loginButton)
        submitButton.assertIsNotEnabled()

        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("test@example.com")
        fields[1].performTextInput("123456")

        submitButton.assertIsEnabled().performClick()

        assertEquals("test@example.com", loginEmail)
        assertEquals("123456", loginPassword)
    }

    @Test
    fun registerScreen_showsPasswordMismatch_andSubmitsTrimmedName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val strings = appStringsFor(context, AppLanguage.RU)
        var submittedName: String? = null
        var submittedEmail: String? = null
        var submittedPassword: String? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides strings) {
                EcoTraceTheme(darkTheme = false, dynamicColor = false) {
                    RegisterScreen(
                        contentPadding = PaddingValues(),
                        onOpenLogin = {},
                        onRegister = { name, email, password ->
                            submittedName = name
                            submittedEmail = email
                            submittedPassword = password
                        },
                    )
                }
            }
        }

        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("  Ivan  ")
        fields[1].performTextInput("user@mail.com")
        fields[2].performTextInput("123456")
        fields[3].performTextInput("654321")

        composeRule.onNodeWithText(strings.passwordsDontMatch).assertIsDisplayed()
        composeRule.onNodeWithText(strings.registerButton).assertIsNotEnabled()

        fields[3].performTextClearance()
        fields[3].performTextInput("123456")

        composeRule.onNodeWithText(strings.registerButton).assertIsEnabled().performClick()

        assertEquals("Ivan", submittedName)
        assertEquals("user@mail.com", submittedEmail)
        assertEquals("123456", submittedPassword)
    }
}
