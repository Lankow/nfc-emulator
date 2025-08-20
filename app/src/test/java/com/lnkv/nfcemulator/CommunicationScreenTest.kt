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
            CommunicationScreen(entries, currentScenario = null, onClearScenario = {})
        }

        composeTestRule.onNodeWithText("0102", substring = true).assertExists()
        composeTestRule.onNodeWithText("A1B2", substring = true).assertExists()
    }

    @Test
    fun serverExpandsWhenNfcHidden() {
        val entries = listOf(
            CommunicationLog.Entry("0102", true),
            CommunicationLog.Entry("A1B2", false)
        )

        composeTestRule.setContent {
            CommunicationScreen(entries, currentScenario = null, onClearScenario = {})
        }

        val heightBoth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.height
        composeTestRule.onNodeWithTag("NfcToggle").performClick()
        composeTestRule.waitForIdle()
        val heightSingle = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.height
        assertTrue(heightSingle > heightBoth)
        composeTestRule.onNodeWithText("A1B2", substring = true).assertDoesNotExist()
    }

    @Test
    fun dividerMatchesLogWidth() {
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, onClearScenario = {}) }

        val logWidth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.width
        val dividerWidth = composeTestRule.onNodeWithTag("ToggleDivider").fetchSemanticsNode().size.width

        assertEquals(logWidth, dividerWidth)
    }

    @Test
    fun actionButtonsDisplayed() {
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, onClearScenario = {}) }
        composeTestRule.onNodeWithTag("SaveButton").assertExists()
        composeTestRule.onNodeWithTag("ClearButton").assertExists()
    }

    @Test
    fun actionButtonsMatchLogWidth() {
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, onClearScenario = {}) }

        val logWidth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.width
        val spacing = with(composeTestRule.density) { 8.dp.roundToPx() }
        val expected = (logWidth - spacing) / 2
        val saveWidth = composeTestRule.onNodeWithTag("SaveButton").fetchSemanticsNode().size.width
        val clearWidth = composeTestRule.onNodeWithTag("ClearButton").fetchSemanticsNode().size.width

        assertEquals(expected, saveWidth)
        assertEquals(expected, clearWidth)
    }

    @Test
    fun segmentsFillWidth() {
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, onClearScenario = {}) }

        val rootWidth = composeTestRule.onRoot().fetchSemanticsNode().size.width
        val expectedWidth = rootWidth - with(composeTestRule.density) { 32.dp.roundToPx() }
        val segWidth = composeTestRule.onNodeWithTag("CommSegments").fetchSemanticsNode().size.width

        assertEquals(expectedWidth, segWidth)
    }

    @Test
    fun lastSegmentDisables() {
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, onClearScenario = {}) }
        composeTestRule.onNodeWithTag("NfcToggle").performClick()
        composeTestRule.onNodeWithTag("ServerToggle").assertIsNotEnabled()
    }

    @Test
    fun scenarioClearButtonShown() {
        composeTestRule.setContent {
            CommunicationScreen(emptyList(), currentScenario = "S1", onClearScenario = {})
        }
        composeTestRule.onNodeWithTag("ScenarioClearButton").assertExists()
    }
}

