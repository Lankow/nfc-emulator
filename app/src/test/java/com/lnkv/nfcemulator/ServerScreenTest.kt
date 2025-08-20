package com.lnkv.nfcemulator

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
    fun disallowsInvalidCharacters() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("IpClear").performClick()
        composeTestRule.onNodeWithTag("IpField").performTextInput("192.168.0.1:a")
        composeTestRule.onNodeWithTag("IpField").assertTextEquals("192.168.0.1:")
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
    fun connectButtonRequiresWifi() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Disconnected")
        composeTestRule.onNodeWithTag("ConnectButton").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Disconnected")
    }

    @Test
    fun externalFieldsRemainEnabledOnFailedConnection() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("ConnectButton").performClick()
        composeTestRule.onNodeWithTag("IpField").assertIsEnabled()
        composeTestRule.onNodeWithTag("PollingField").assertIsEnabled()
        composeTestRule.onNodeWithTag("AutoConnectCheck").assertIsEnabled()
        composeTestRule.onNodeWithTag("SaveServer").assertIsEnabled()
    }

    @Test
    fun connectRequiresInputs() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("IpClear").performClick()
        composeTestRule.onNodeWithTag("PollingClear").performClick()
        composeTestRule.onNodeWithTag("ConnectButton").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Disconnected")
    }

    @Test
    fun connectRequiresPort() {
        composeTestRule.setContent { ServerScreen() }
        val field = composeTestRule.onNodeWithTag("IpField")
        field.performTextClearance()
        field.performTextInput("192.168.0.1")
        composeTestRule.onNodeWithTag("ConnectButton").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Disconnected")
    }

    @Test
    fun staticPortCheckboxEnablesField() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("InternalToggle").performClick()
        val portField = composeTestRule.onNodeWithTag("PortField")
        portField.assertIsNotEnabled()
        composeTestRule.onNodeWithTag("StaticPortCheck").performClick()
        portField.assertIsEnabled()
    }

    @Test
    fun startButtonTogglesState() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("InternalToggle").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Stopped")
        composeTestRule.onNodeWithTag("StartButton").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Running")
        composeTestRule.onNodeWithTag("StartButton").performClick()
        composeTestRule.onNodeWithTag("ServerState").assertTextEquals("Server State: Stopped")
    }

    @Test
    fun internalControlsDisabledWhenRunning() {
        composeTestRule.setContent { ServerScreen() }
        composeTestRule.onNodeWithTag("InternalToggle").performClick()
        composeTestRule.onNodeWithTag("StartButton").performClick()
        composeTestRule.onNodeWithTag("StaticPortCheck").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("AutoStartCheck").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("PortField").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("SaveServer").assertIsNotEnabled()
    }
}
