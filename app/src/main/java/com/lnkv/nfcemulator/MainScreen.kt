package com.lnkv.nfcemulator

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainScreen() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Communication) }
    val logEntries by CommunicationLog.entries.collectAsState()
    val currentScenario by ScenarioManager.current.collectAsState()
    val isRunning by ScenarioManager.running.collectAsState()
    val isSilenced by ScenarioManager.silenced.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = currentScreen != Screen.Communication) {
        currentScreen = Screen.Communication
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            when (screen) {
                                Screen.Communication -> Icon(Icons.Filled.Nfc, contentDescription = screen.label)
                                Screen.Scenario -> Icon(Icons.Filled.List, contentDescription = screen.label)
                                Screen.Server -> Icon(Icons.Filled.Wifi, contentDescription = screen.label)
                                Screen.Aid -> Icon(Icons.Filled.Edit, contentDescription = screen.label)
                            }
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (currentScreen) {
            Screen.Communication ->
                CommunicationScreen(
                    logEntries,
                    currentScenario,
                    isRunning,
                    isSilenced,
                    onToggleRun = {
                        if (currentScenario != null) {
                            val starting = !isRunning
                            ScenarioManager.setRunning(starting)
                        }
                    },
                    onClearScenario = {
                        ScenarioManager.setCurrent(context, null)
                        ScenarioManager.setRunning(false)
                    },
                    onToggleSilence = { ScenarioManager.toggleSilence() },
                    modifier = Modifier.padding(padding)
                )
            Screen.Scenario ->
                ScenarioScreen(
                    modifier = Modifier.padding(padding),
                    onPlayScenario = { currentScreen = Screen.Communication }
                )
            Screen.Server ->
                ServerScreen(Modifier.padding(padding))
            Screen.Aid ->
                AidScreen(Modifier.padding(padding))
        }
    }
}

enum class Screen(val label: String) {
    Communication("Comm"),
    Scenario("Scenarios"),
    Server("Server"),
    Aid("AID")
}
