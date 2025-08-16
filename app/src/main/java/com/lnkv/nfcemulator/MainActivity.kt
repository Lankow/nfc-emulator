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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
                            Screen.entries.forEach { screen ->
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().testTag("IncomingToggle")
        ) {
            Checkbox(
                checked = showIncoming,
                onCheckedChange = { checked ->
                    if (!checked && !showOutgoing) return@Checkbox
                    showIncoming = checked
                }
            )
            Text("Incoming Communication")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().testTag("OutgoingToggle")
        ) {
            Checkbox(
                checked = showOutgoing,
                onCheckedChange = { checked ->
                    if (!checked && !showIncoming) return@Checkbox
                    showOutgoing = checked
                }
            )
            Text("Outgoing Communication")
        }

        Spacer(modifier = Modifier.height(4.dp))
        Divider(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.CenterHorizontally)
                .testTag("ToggleDivider")
        )
        Spacer(modifier = Modifier.height(8.dp))

        val incomingEntries = entries.filter { it.isRequest }
        val outgoingEntries = entries.filter { !it.isRequest }

        when {
            showIncoming && showOutgoing -> {
                CommunicationLogList(
                    label = "Incoming Communication",
                    entries = incomingEntries,
                    tag = "IncomingLog",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                CommunicationLogList(
                    label = "Outgoing Communication",
                    entries = outgoingEntries,
                    tag = "OutgoingLog",
                    modifier = Modifier.weight(1f)
                )
            }
            showIncoming -> {
                CommunicationLogList(
                    label = "Incoming Communication",
                    entries = incomingEntries,
                    tag = "IncomingLog",
                    modifier = Modifier.weight(1f)
                )
            }
            showOutgoing -> {
                CommunicationLogList(
                    label = "Outgoing Communication",
                    entries = outgoingEntries,
                    tag = "OutgoingLog",
                    modifier = Modifier.weight(1f)
                )
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

@Composable
private fun CommunicationLogList(
    label: String,
    entries: List<CommunicationLog.Entry>,
    tag: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text(label)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag(tag)
                .padding(8.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries) { entry ->
                    val color = if (entry.isRequest) Color.Red else Color.Green
                    Text(entry.message, color = color)
                }
            }
        }
    }
}

