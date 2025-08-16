package com.lnkv.nfcemulator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

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
}
