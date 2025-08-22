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
        composeTestRule.onNodeWithContentDescription("Communication").assertExists()
        composeTestRule.onNodeWithContentDescription("Scenarios").assertExists()
        composeTestRule.onNodeWithContentDescription("Server").assertExists()
        composeTestRule.onNodeWithContentDescription("AID").assertExists()
    }
}
