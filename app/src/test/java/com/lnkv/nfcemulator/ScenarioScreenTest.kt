package com.lnkv.nfcemulator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertTextEquals
import org.junit.Rule
import org.junit.Test

class ScenarioScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun commandButtonsDisplayedAndClearWorks() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("CommandSend").assertExists()
        composeTestRule.onNodeWithTag("CommandClear").assertExists()

        composeTestRule.onNodeWithTag("CommandField").performTextInput("AB")
        composeTestRule.onNodeWithTag("CommandClear").performClick()
        composeTestRule.onNodeWithTag("CommandField").assertTextEquals("")
    }

    @Test
    fun titleRejectsInvalidCharacters() {
        composeTestRule.setContent { ScenarioScreen() }
        composeTestRule.onNodeWithTag("TitleField").performTextInput("test/")
        composeTestRule.onNodeWithTag("TitleField").assertTextEquals("test")
    }
}
