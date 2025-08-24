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
            CommunicationScreen(entries, currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {})
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
            CommunicationScreen(entries, currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {})
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
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {}) }

        val logWidth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.width
        val dividerWidth = composeTestRule.onNodeWithTag("ToggleDivider").fetchSemanticsNode().size.width

        assertEquals(logWidth, dividerWidth)
    }

    @Test
    fun actionButtonsDisplayed() {
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {}) }
        composeTestRule.onNodeWithTag("SaveButton").assertExists()
        composeTestRule.onNodeWithTag("ClearButton").assertExists()
    }

    @Test
    fun actionButtonsMatchLogWidth() {
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {}) }

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
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {}) }

        val rootWidth = composeTestRule.onRoot().fetchSemanticsNode().size.width
        val expectedWidth = rootWidth - with(composeTestRule.density) { 32.dp.roundToPx() }
        val segWidth = composeTestRule.onNodeWithTag("CommSegments").fetchSemanticsNode().size.width

        assertEquals(expectedWidth, segWidth)
    }

    @Test
    fun lastSegmentDisables() {
        composeTestRule.setContent { CommunicationScreen(emptyList(), currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {}) }
        composeTestRule.onNodeWithTag("NfcToggle").performClick()
        composeTestRule.onNodeWithTag("ServerToggle").assertIsNotEnabled()
    }

    @Test
    fun scenarioClearButtonVisibility() {
        composeTestRule.setContent {
            CommunicationScreen(emptyList(), currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {})
        }
        composeTestRule.onNodeWithTag("ScenarioClearButton").assertDoesNotExist()

        composeTestRule.setContent {
            CommunicationScreen(emptyList(), currentScenario = "S1", isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {})
        }
        composeTestRule.onNodeWithTag("ScenarioClearButton").assertExists()
    }

    @Test
    fun scenarioRunButtonVisibility() {
        composeTestRule.setContent {
            CommunicationScreen(emptyList(), currentScenario = null, isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {})
        }
        composeTestRule.onNodeWithTag("ScenarioRunButton").assertDoesNotExist()

        composeTestRule.setContent {
            CommunicationScreen(emptyList(), currentScenario = "S1", isRunning = false, isSilenced = false, onToggleRun = {}, onClearScenario = {}, onToggleSilence = {})
        }
        composeTestRule.onNodeWithTag("ScenarioRunButton").assertExists().assertIsEnabled()

        composeTestRule.onNodeWithTag("ScenarioSilenceButton").assertExists()
    }
}

