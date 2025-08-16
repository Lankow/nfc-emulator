package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import com.lnkv.nfcemulator.cardservice.TypeAEmulatorService
import com.lnkv.nfcemulator.ui.theme.NFCEmulatorTheme

/**
 * Main activity hosting a bottom navigation menu that switches between
 * Communication, Server, and Settings screens.
 */
class MainActivity : ComponentActivity() {
    private lateinit var cardEmulation: CardEmulation
    private lateinit var componentName: ComponentName
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        cardEmulation = CardEmulation.getInstance(nfcAdapter)
        componentName = ComponentName(this, TypeAEmulatorService::class.java)
        prefs = getSharedPreferences("nfc_aids", MODE_PRIVATE)

        val storedAids = prefs.getStringSet("aids", setOf("F0010203040506"))!!.toList()
        registerAids(storedAids)

        setContent {
            NFCEmulatorTheme {
                var currentScreen by rememberSaveable { mutableStateOf(Screen.Communication) }
                val logEntries by CommunicationLog.entries.collectAsState()

                androidx.compose.material3.Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Screen.values().forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = { Box(modifier = Modifier.height(24.dp)) },
                                    label = { Text(screen.label) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    when (currentScreen) {
                        Screen.Communication ->
                            CommunicationScreen(logEntries, Modifier.padding(padding))
                        Screen.Server ->
                            PlaceholderScreen("Server", Modifier.padding(padding))
                        Screen.Settings ->
                            PlaceholderScreen("Settings", Modifier.padding(padding))
                    }
                }
            }
        }
    }

    private fun registerAids(aids: List<String>) {
        cardEmulation.registerAidsForService(
            componentName,
            CardEmulation.CATEGORY_OTHER,
            aids
        )
    }
}

enum class Screen(val label: String) {
    Communication("Communication"),
    Server("Server"),
    Settings("Settings")
}

@Composable
fun CommunicationScreen(
    entries: List<CommunicationLog.Entry>,
    modifier: Modifier = Modifier
) {
    var showIncoming by rememberSaveable { mutableStateOf(true) }
    var showOutgoing by rememberSaveable { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = showIncoming,
                onCheckedChange = { checked ->
                    if (!checked && !showOutgoing) return@Checkbox
                    showIncoming = checked
                }
            )
            Text("Show Incoming Communication")
        }
        if (showIncoming) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(entries.filter { it.isRequest }) { entry ->
                    Text(entry.message, color = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = showOutgoing,
                onCheckedChange = { checked ->
                    if (!checked && !showIncoming) return@Checkbox
                    showOutgoing = checked
                }
            )
            Text("Show Outgoing Communication")
        }
        if (showOutgoing) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(entries.filter { !it.isRequest }) { entry ->
                    Text(entry.message, color = Color.Green)
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Center) {
        Text(text)
    }
}

