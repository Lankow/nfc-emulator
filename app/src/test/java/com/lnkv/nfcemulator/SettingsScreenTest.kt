package com.lnkv.nfcemulator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreenDisplaysOptions() {
        composeTestRule.setContent { SettingsScreen() }
        composeTestRule.onNodeWithTag("MultiSelectToggle").assertExists()
        composeTestRule.onNodeWithTag("SelectedRespSpinner").assertExists()
        composeTestRule.onNodeWithTag("UnselectedRespSpinner").assertExists()
    }
}
