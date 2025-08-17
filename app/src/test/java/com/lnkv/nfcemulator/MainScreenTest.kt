package com.lnkv.nfcemulator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navigationIconsPresent() {
        composeTestRule.setContent { MainScreen() }
        composeTestRule.onNodeWithContentDescription("Comm").assertExists()
        composeTestRule.onNodeWithContentDescription("Scenario").assertExists()
        composeTestRule.onNodeWithContentDescription("Server").assertExists()
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
    }
}
