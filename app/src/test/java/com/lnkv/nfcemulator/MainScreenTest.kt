package com.lnkv.nfcemulator

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import com.lnkv.nfcemulator.ui.theme.NFCEmulatorTheme

class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun headerUpdatesWithNavigation() {
        composeTestRule.setContent { MainScreen() }

        composeTestRule.onNodeWithTag("ScreenHeader").assertExists()
        composeTestRule.onNodeWithTag("ScreenHeader").assertTextEquals("Communication")

        composeTestRule.onNodeWithText("Server").performClick()
        composeTestRule.onNodeWithTag("ScreenHeader").assertTextEquals("Server")

        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithTag("ScreenHeader").assertTextEquals("Settings")
    }

    @Test
    fun topBarUsesPrimaryColor() {
        var expected = Color.Unspecified
        composeTestRule.setContent {
            NFCEmulatorTheme {
                expected = MaterialTheme.colorScheme.primary
                MainScreen()
            }
        }

        val image = composeTestRule.onNodeWithTag("TopBar").captureToImage()
        val actual = image.toPixelMap()[0, 0]
        assertEquals(expected.toArgb(), actual.toArgb())
    }
}
