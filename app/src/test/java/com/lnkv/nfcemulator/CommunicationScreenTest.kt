package com.lnkv.nfcemulator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CommunicationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsServerAndNfcMessages() {
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

    @Test
    fun serverExpandsWhenNfcHidden() {
        val entries = listOf(
            CommunicationLog.Entry("0102", true),
            CommunicationLog.Entry("A1B2", false)
        )

        composeTestRule.setContent {
            CommunicationScreen(entries)
        }

        val heightBoth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.height
        composeTestRule.onNodeWithText("NFC Communication").performClick()
        composeTestRule.waitForIdle()
        val heightSingle = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.height
        assertTrue(heightSingle > heightBoth)
        composeTestRule.onNodeWithText("A1B2").assertDoesNotExist()
    }

    @Test
    fun toggleRowsFillWidth() {
        composeTestRule.setContent { CommunicationScreen(emptyList()) }

        val rootWidth = composeTestRule.onRoot().fetchSemanticsNode().size.width
        val expectedWidth = rootWidth - with(composeTestRule.density) { 32.dp.roundToPx() }

        val serverWidth = composeTestRule.onNodeWithTag("ServerToggle").fetchSemanticsNode().size.width
        val nfcWidth = composeTestRule.onNodeWithTag("NfcToggle").fetchSemanticsNode().size.width

        assertEquals(expectedWidth, serverWidth)
        assertEquals(expectedWidth, nfcWidth)
    }

    @Test
    fun dividerMatchesLogWidth() {
        composeTestRule.setContent { CommunicationScreen(emptyList()) }

        val logWidth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.width
        val dividerWidth = composeTestRule.onNodeWithTag("ToggleDivider").fetchSemanticsNode().size.width

        assertEquals(logWidth, dividerWidth)
    }

    @Test
    fun togglesAlignWithLogs() {
        composeTestRule.setContent { CommunicationScreen(emptyList()) }

        val serverCheckX = composeTestRule.onNodeWithTag("ServerCheck").fetchSemanticsNode().positionInRoot.x
        val serverLogX = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().positionInRoot.x

        val nfcCheckX = composeTestRule.onNodeWithTag("NfcCheck").fetchSemanticsNode().positionInRoot.x
        val nfcLogX = composeTestRule.onNodeWithTag("NfcLog").fetchSemanticsNode().positionInRoot.x

        assertEquals(serverLogX, serverCheckX)
        assertEquals(nfcLogX, nfcCheckX)
    }

    @Test
    fun saveButtonIsDisplayed() {
        composeTestRule.setContent { CommunicationScreen(emptyList()) }
        composeTestRule.onNodeWithTag("SaveButton").assertExists()
    }
}

