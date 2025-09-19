package com.lnkv.nfcemulator

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommunicationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        CommunicationFilter.clear()
    }

    private fun setScreen(
        entries: List<CommunicationLog.Entry> = emptyList(),
        currentScenario: String? = null,
        isRunning: Boolean = false,
        isSilenced: Boolean = false,
        isNfcEnabled: Boolean = true
    ) {
        composeTestRule.setContent {
            CommunicationScreen(
                entries = entries,
                currentScenario = currentScenario,
                isRunning = isRunning,
                isSilenced = isSilenced,
                isNfcEnabled = isNfcEnabled,
                onToggleRun = {},
                onClearScenario = {},
                onToggleSilence = {},
                onToggleNfc = {}
            )
        }
    }

    @Test
    fun showsServerAndNfcMessages() {
        val entries = listOf(
            CommunicationLog.Entry("0102", true),
            CommunicationLog.Entry("A1B2", false)
        )

        setScreen(entries = entries)

        composeTestRule.onNodeWithText("0102", substring = true).assertExists()
        composeTestRule.onNodeWithText("A1B2", substring = true).assertExists()
    }

    @Test
    fun serverExpandsWhenNfcHidden() {
        val entries = listOf(
            CommunicationLog.Entry("0102", true),
            CommunicationLog.Entry("A1B2", false)
        )

        setScreen(entries = entries)

        val heightBoth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.height
        composeTestRule.onNodeWithTag("NfcToggle").performClick()
        composeTestRule.waitForIdle()
        val heightSingle = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.height
        assertTrue(heightSingle > heightBoth)
        composeTestRule.onNodeWithText("A1B2", substring = true).assertDoesNotExist()
    }

    @Test
    fun dividerMatchesLogWidth() {
        setScreen()

        val logWidth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.width
        val dividerWidth = composeTestRule.onNodeWithTag("ToggleDivider").fetchSemanticsNode().size.width

        assertEquals(logWidth, dividerWidth)
    }

    @Test
    fun actionButtonsDisplayed() {
        setScreen()
        composeTestRule.onNodeWithTag("LogsButton").assertExists()
        composeTestRule.onNodeWithTag("FilterButton").assertExists()
        composeTestRule.onNodeWithTag("ClearButton").assertExists()
    }

    @Test
    fun actionButtonsMatchLogWidth() {
        setScreen()

        val logWidth = composeTestRule.onNodeWithTag("ServerLog").fetchSemanticsNode().size.width
        val spacing = with(composeTestRule.density) { 16.dp.roundToPx() }
        val expected = (logWidth - spacing) / 3
        val logsWidth = composeTestRule.onNodeWithTag("LogsButton").fetchSemanticsNode().size.width
        val filterWidth = composeTestRule.onNodeWithTag("FilterButton").fetchSemanticsNode().size.width
        val clearWidth = composeTestRule.onNodeWithTag("ClearButton").fetchSemanticsNode().size.width

        assertEquals(expected, logsWidth)
        assertEquals(expected, filterWidth)
        assertEquals(expected, clearWidth)
    }

    @Test
    fun segmentsFillWidth() {
        setScreen()

        val rootWidth = composeTestRule.onRoot().fetchSemanticsNode().size.width
        val expectedWidth = rootWidth - with(composeTestRule.density) { 32.dp.roundToPx() }
        val segWidth = composeTestRule.onNodeWithTag("CommSegments").fetchSemanticsNode().size.width

        assertEquals(expectedWidth, segWidth)
    }

    @Test
    fun lastSegmentDisables() {
        setScreen()
        composeTestRule.onNodeWithTag("NfcToggle").performClick()
        composeTestRule.onNodeWithTag("ServerToggle").assertIsNotEnabled()
    }

    @Test
    fun scenarioClearButtonVisibility() {
        setScreen(currentScenario = null)
        composeTestRule.onNodeWithTag("ScenarioClearButton").assertDoesNotExist()

        setScreen(currentScenario = "S1")
        composeTestRule.onNodeWithTag("ScenarioClearButton").assertExists()
    }

    @Test
    fun scenarioRunButtonVisibility() {
        setScreen(currentScenario = null)
        composeTestRule.onNodeWithTag("ScenarioRunButton").assertDoesNotExist()

        setScreen(currentScenario = "S1")
        composeTestRule.onNodeWithTag("ScenarioRunButton").assertExists().assertIsEnabled()

        composeTestRule.onNodeWithTag("ScenarioSilenceButton").assertExists()
        composeTestRule.onNodeWithTag("ScenarioNfcButton").assertExists()
    }

    @Test
    fun scenarioNfcButtonReflectsState() {
        setScreen(currentScenario = "S1", isNfcEnabled = true)
        composeTestRule.onNodeWithContentDescription("Disable NFC").assertExists()

        setScreen(currentScenario = "S1", isNfcEnabled = false)
        composeTestRule.onNodeWithContentDescription("Enable NFC").assertExists()
    }
}
