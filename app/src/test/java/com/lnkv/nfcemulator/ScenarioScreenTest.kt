package com.lnkv.nfcemulator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertDoesNotExist
import org.junit.Rule
import org.junit.Test

class ScenarioScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editIconOpensEditorWhenScenarioSelected() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("ScenarioTitle").performTextInput("S1")
        composeTestRule.onNodeWithTag("ScenarioAid").performTextInput("A0000002471001")
        composeTestRule.onNodeWithTag("ScenarioSave").performClick()
        composeTestRule.onNodeWithTag("ScenarioItem0").performClick()
        composeTestRule.onNodeWithTag("ScenarioEdit0").performClick()
        composeTestRule.onNodeWithTag("ScenarioTitle").assertExists()
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
    fun aidFieldVisible() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("ScenarioAid").assertExists()
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
        composeTestRule.onNodeWithTag("ScenarioAid").performTextInput("A0000002471001")
        composeTestRule.onNodeWithTag("ScenarioSave").performClick()
        composeTestRule.onNodeWithTag("ScenarioItem0").performClick()
        composeTestRule.onNodeWithTag("ScenarioPlay0").performClick()
        composeTestRule.onNodeWithTag("ScenarioItem0").assertExists()
    }

    @Test
    fun allowsSpacesInNames() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("ScenarioTitle").performTextInput("My Scenario")
        composeTestRule.onNodeWithTag("ScenarioTitle").assertTextEquals("My Scenario")
        composeTestRule.onNodeWithTag("StepNew").performClick()
        composeTestRule.onNodeWithTag("StepName").performTextInput("Step One")
        composeTestRule.onNodeWithTag("StepName").assertTextEquals("Step One")
    }

    @Test
    fun editIconOpensStepEditor() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("StepNew").performClick()
        composeTestRule.onNodeWithTag("StepName").performTextInput("S1")
        composeTestRule.onNodeWithTag("StepSave").performClick()
        composeTestRule.onNodeWithTag("StepEdit0").performClick()
        composeTestRule.onNodeWithTag("StepName").assertExists()
    }

    @Test
    fun deleteIconRemovesStep() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("ScenarioNew").performClick()
        composeTestRule.onNodeWithTag("StepNew").performClick()
        composeTestRule.onNodeWithTag("StepName").performTextInput("S1")
        composeTestRule.onNodeWithTag("StepSave").performClick()
        composeTestRule.onNodeWithTag("StepDelete0").performClick()
        composeTestRule.onNodeWithTag("StepItem0").assertDoesNotExist()
    }
}
