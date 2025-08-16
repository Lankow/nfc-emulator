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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
                MainScreen()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Communication) }
    val logEntries by CommunicationLog.entries.collectAsState()

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(title = { Text(currentScreen.label, modifier = Modifier.testTag("ScreenHeader")) })
        },
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

@Composable
fun CommunicationScreen(
    entries: List<CommunicationLog.Entry>,
    modifier: Modifier = Modifier
) {
    var showServer by rememberSaveable { mutableStateOf(true) }
    var showNfc by rememberSaveable { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().testTag("ServerToggle")
        ) {
            Checkbox(
                checked = showServer,
                onCheckedChange = { checked ->
                    if (!checked && !showNfc) return@Checkbox
                    showServer = checked
                }
            )
            Text("Server Communication")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().testTag("NfcToggle")
        ) {
            Checkbox(
                checked = showNfc,
                onCheckedChange = { checked ->
                    if (!checked && !showServer) return@Checkbox
                    showNfc = checked
                }
            )
            Text("NFC Communication")
        }

        Spacer(modifier = Modifier.height(4.dp))
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .testTag("ToggleDivider")
        )
        Spacer(modifier = Modifier.height(8.dp))

        val serverEntries = entries.filter { it.isRequest }
        val nfcEntries = entries.filter { !it.isRequest }

        when {
            showServer && showNfc -> {
                CommunicationLogList(
                    label = "Server Communication",
                    entries = serverEntries,
                    tag = "ServerLog",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                CommunicationLogList(
                    label = "NFC Communication",
                    entries = nfcEntries,
                    tag = "NfcLog",
                    modifier = Modifier.weight(1f)
                )
            }
            showServer -> {
                CommunicationLogList(
                    label = "Server Communication",
                    entries = serverEntries,
                    tag = "ServerLog",
                    modifier = Modifier.weight(1f)
                )
            }
            showNfc -> {
                CommunicationLogList(
                    label = "NFC Communication",
                    entries = nfcEntries,
                    tag = "NfcLog",
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

