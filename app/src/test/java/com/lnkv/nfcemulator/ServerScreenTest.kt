package com.lnkv.nfcemulator

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ServerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ipClearWorks() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("IpClear").performClick()
        composeTestRule.onNodeWithTag("IpField").assertTextEquals("")
    }

    @Test
    fun pollingClearWorks() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("PollingClear").performClick()
        composeTestRule.onNodeWithTag("PollingField").assertTextEquals("")
    }

    @Test
    fun disallowsLetters() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("IpClear").performClick()
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

    @Test
    fun pollingFieldRestrictsInput() {
        composeTestRule.setContent { ServerScreen() }
        val field = composeTestRule.onNodeWithTag("PollingField")
        field.performTextClearance()
        field.performTextInput("123a")
        field.assertTextEquals("123")
        field.performTextClearance()
        field.performTextInput("10001")
        field.assertTextEquals("1000")
    }

    @Test
    fun autoConnectCheckboxToggles() {
        composeTestRule.setContent { ServerScreen() }
        val check = composeTestRule.onNodeWithTag("AutoConnectCheck")
        check.assertIsOff()
        check.performClick()
        check.assertIsOn()
    }

    @Test
    fun connectButtonTogglesState() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Disconnected")
        composeTestRule.onNodeWithTag("ConnectButton").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Connected")
        composeTestRule.onNodeWithTag("ConnectButton").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Disconnected")
    }

    @Test
    fun connectRequiresInputs() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("IpClear").performClick()
        composeTestRule.onNodeWithTag("PollingClear").performClick()
        composeTestRule.onNodeWithTag("ConnectButton").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Disconnected")
    }
}
