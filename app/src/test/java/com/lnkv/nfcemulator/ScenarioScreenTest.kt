package com.lnkv.nfcemulator

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertExists
import org.junit.Rule
import org.junit.Test

class ScenarioScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editButtonEnabledWhenScenarioSelected() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioEdit").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("ScenarioTitle").performTextInput("S1")
        composeTestRule.onNodeWithTag("ScenarioSave").performClick()
        composeTestRule.onNodeWithTag("ScenarioItem0").performClick()
        composeTestRule.onNodeWithTag("ScenarioEdit").assertIsEnabled()
    }

    @Test
    fun newButtonOpensEditor() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("ScenarioTitle").assertExists()
    }

    @Test
    fun newStepOpensStepEditor() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("StepNew").performClick()
        composeTestRule.onNodeWithTag("StepName").assertExists()
    }

    @Test
    fun scenarioListHasBackgroundContainer() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioList").assertExists()
    }

    @Test
    fun scenarioRemainsAfterPlay() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("ScenarioTitle").performTextInput("S1")
        composeTestRule.onNodeWithTag("ScenarioSave").performClick()
        composeTestRule.onNodeWithTag("ScenarioItem0").performClick()
        composeTestRule.onNodeWithTag("ScenarioPlay0").performClick()
        composeTestRule.onNodeWithTag("ScenarioItem0").assertExists()
    }
}
