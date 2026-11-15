package com.example

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.ABOUT_GITHUB_URL
import com.example.ui.components.ABOUT_UPI_ID
import com.example.ui.components.AboutAppDialog
import com.example.ui.components.openUriOrCopyFallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AboutDialogRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `AboutAppDialog shows only Star on GitHub and Donate by UPI cards without raw links or copy buttons`() {
        composeTestRule.setContent {
            MaterialTheme {
                AboutAppDialog(onDismiss = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("about_github_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_donate_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Star on GitHub").assertIsDisplayed()
        composeTestRule.onNodeWithText("Donate by UPI").assertIsDisplayed()

        composeTestRule.onNodeWithText(ABOUT_GITHUB_URL).assertDoesNotExist()
        composeTestRule.onNodeWithText(ABOUT_UPI_ID).assertDoesNotExist()
        composeTestRule.onNodeWithText("Copy").assertDoesNotExist()
        composeTestRule.onNodeWithText("Pay via UPI").assertDoesNotExist()
    }

    @Test
    fun `clicking Star on GitHub falls back to copying link and shows Toast when browser fails to open`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        org.robolectric.shadows.ShadowToast.reset()
        val failingUriHandler = object : androidx.compose.ui.platform.UriHandler {
            override fun openUri(uri: String) {
                throw ActivityNotFoundException("No browser installed")
            }
        }

        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalUriHandler provides failingUriHandler
            ) {
                MaterialTheme {
                    AboutAppDialog(onDismiss = {})
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("about_github_card").performClick()
        composeTestRule.waitForIdle()

        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(ABOUT_GITHUB_URL, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertEquals("GitHub link copied to clipboard", org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `clicking Donate by UPI falls back to copying UPI ID and shows Toast when no handler exists`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        org.robolectric.Shadows.shadowOf(app).checkActivities(true)
        ShadowToast.reset()

        composeTestRule.setContent {
            MaterialTheme {
                AboutAppDialog(onDismiss = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("about_donate_card").performClick()
        composeTestRule.waitForIdle()

        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(ABOUT_UPI_ID, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertEquals("UPI ID copied to clipboard", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `openUriOrCopyFallback launches URI when possible and copies with Toast when launch fails`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        ShadowToast.reset()

        var openedUri: String? = null
        val opened = openUriOrCopyFallback(
            context = app,
            uriString = ABOUT_GITHUB_URL,
            clipboardLabel = "Star on GitHub",
            fallbackCopyText = ABOUT_GITHUB_URL,
            fallbackToastText = "GitHub link copied to clipboard",
            launchAction = { uri -> openedUri = uri.toString() }
        )
        assertTrue(opened)
        assertEquals(ABOUT_GITHUB_URL, openedUri)
        assertEquals(null, ShadowToast.getTextOfLatestToast())

        val fellBack = openUriOrCopyFallback(
            context = app,
            uriString = ABOUT_GITHUB_URL,
            clipboardLabel = "Star on GitHub",
            fallbackCopyText = ABOUT_GITHUB_URL,
            fallbackToastText = "GitHub link copied to clipboard",
            launchAction = { throw ActivityNotFoundException("No browser installed") }
        )
        assertFalse(fellBack)
        assertEquals(ABOUT_GITHUB_URL, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertEquals("GitHub link copied to clipboard", ShadowToast.getTextOfLatestToast())
    }
}
