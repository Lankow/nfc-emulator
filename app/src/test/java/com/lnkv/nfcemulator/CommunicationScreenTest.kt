package com.lnkv.nfcemulator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.math.roundToInt

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

    @Test
    fun incomingExpandsWhenOutgoingHidden() {
        val entries = listOf(
            CommunicationLog.Entry("0102", true),
            CommunicationLog.Entry("A1B2", false)
        )

        composeTestRule.setContent {
            CommunicationScreen(entries)
        }

        val heightBoth = composeTestRule.onNodeWithTag("IncomingLog").fetchSemanticsNode().size.height
        composeTestRule.onNodeWithText("Outgoing Communication").performClick()
        composeTestRule.waitForIdle()
        val heightSingle = composeTestRule.onNodeWithTag("IncomingLog").fetchSemanticsNode().size.height
        assertTrue(heightSingle > heightBoth)
        composeTestRule.onNodeWithText("A1B2").assertDoesNotExist()
    }

    @Test
    fun toggleRowsFillWidth() {
        composeTestRule.setContent { CommunicationScreen(emptyList()) }

        val rootWidth = composeTestRule.onRoot().fetchSemanticsNode().size.width
        val expectedWidth = rootWidth - with(composeTestRule.density) { 32.dp.roundToPx() }

        val incomingWidth = composeTestRule.onNodeWithTag("IncomingToggle").fetchSemanticsNode().size.width
        val outgoingWidth = composeTestRule.onNodeWithTag("OutgoingToggle").fetchSemanticsNode().size.width

        assertEquals(expectedWidth, incomingWidth)
        assertEquals(expectedWidth, outgoingWidth)
    }

    @Test
    fun dividerSpansNinetyPercentWidth() {
        composeTestRule.setContent { CommunicationScreen(emptyList()) }

        val rootWidth = composeTestRule.onRoot().fetchSemanticsNode().size.width
        val contentWidth = rootWidth - with(composeTestRule.density) { 32.dp.roundToPx() }
        val expectedWidth = (contentWidth * 0.9f).roundToInt()

        val dividerWidth = composeTestRule.onNodeWithTag("ToggleDivider").fetchSemanticsNode().size.width

        assertEquals(expectedWidth, dividerWidth)
    }
}

