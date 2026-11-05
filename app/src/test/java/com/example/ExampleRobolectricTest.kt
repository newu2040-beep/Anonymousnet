package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AnonymousnetViewModel
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppFontOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Anonymousnet", appName)
    }

    @Test
    fun `main activity launches and renders welcome screen without crashing`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Anonymousnet").assertExists()
        composeTestRule.onNodeWithText("VERSION 3.0").assertExists()
        composeTestRule.onNodeWithTag("continue_with_google_button").assertExists()
        composeTestRule.onNodeWithTag("anonymous_login_button").assertExists()
        composeTestRule.onNodeWithTag("welcome_theme_toggle").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `verify v3 themes fonts and categories`() {
        assertTrue(AppColorTheme.entries.size >= 16)
        assertTrue(AppFontOption.entries.size >= 7)
        assertTrue(AnonymousnetViewModel.DEFAULT_CATEGORIES.size >= 18)
    }
}

