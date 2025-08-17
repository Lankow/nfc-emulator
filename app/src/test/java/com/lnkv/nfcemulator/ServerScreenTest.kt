package com.lnkv.nfcemulator

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.assertTextEquals
import org.junit.Rule
import org.junit.Test

class ServerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ipButtonsDisplayedAndClearWorks() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("IpApply").assertExists()
        composeTestRule.onNodeWithTag("IpClear").assertExists()

        composeTestRule.onNodeWithTag("IpField").performTextInput("192.168.0.1")
        composeTestRule.onNodeWithTag("IpClear").performClick()
        composeTestRule.onNodeWithTag("IpField").assertTextEquals("")
    }

    @Test
    fun applyDisabledForInvalidIp() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("IpField").performTextInput("999.999.1.1")
        composeTestRule.onNodeWithTag("IpApply").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("IpField").performTextClearance()
        composeTestRule.onNodeWithTag("IpField").performTextInput("192.168.0.1")
        composeTestRule.onNodeWithTag("IpApply").assertIsEnabled()
    }

    @Test
    fun disallowsLetters() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("IpField").performTextInput("192a")
        composeTestRule.onNodeWithTag("IpField").assertTextEquals("192")
    }

    @Test
    fun serverTypeSegmentedButtonsWork() {
        composeTestRule.setContent { ServerScreen() }
        val external = composeTestRule.onNodeWithTag("ExternalToggle")
        val internal = composeTestRule.onNodeWithTag("InternalToggle")
        external.assertIsSelected()
        internal.assertIsNotSelected()
        internal.performClick()
        internal.assertIsSelected()
        external.assertIsNotSelected()
    }
}
