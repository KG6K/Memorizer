package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityLaunchRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `MainActivity launches cleanly and navigates Settings and About`() {
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_full_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_about_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_github_card").assertIsDisplayed()
        composeRule.onNodeWithTag("about_donate_card").assertIsDisplayed()
        composeRule.onNodeWithText("Star on GitHub").assertIsDisplayed()
        composeRule.onNodeWithText("Donate by UPI").assertIsDisplayed()

        composeRule.onNodeWithTag("close_about_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()
    }
}
