package com.lnkv.nfcemulator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class CommunicationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsIncomingAndOutgoingMessages() {
        val entries = listOf(
            CommunicationLog.Entry("0102", true),
            CommunicationLog.Entry("A1B2", false)
        )

        composeTestRule.setContent {
            CommunicationScreen(entries)
        }

        composeTestRule.onNodeWithText("0102").assertExists()
        composeTestRule.onNodeWithText("A1B2").assertExists()
    }
}

