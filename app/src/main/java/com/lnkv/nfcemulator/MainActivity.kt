@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
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
import androidx.compose.foundation.border
import androidx.compose.foundation.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import java.io.File
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

/**
 * Navigation targets displayed in the bottom bar.
 */
enum class Screen(val label: String) {
    Communication("Comm"),
    Scenario("Scenario"),
    Server("Server"),
    Settings("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Communication) }
    val logEntries by CommunicationLog.entries.collectAsState()

    androidx.compose.material3.Scaffold(
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
                                Screen.Settings -> Icon(Icons.Filled.Settings, contentDescription = screen.label)
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
                CommunicationScreen(logEntries, Modifier.padding(padding))
            Screen.Scenario ->
                ScenarioScreen(Modifier.padding(padding))
            Screen.Server ->
                ServerScreen(Modifier.padding(padding))
            Screen.Settings ->
                PlaceholderScreen("Settings", Modifier.padding(padding))
        }
    }
}

/**
 * UI for monitoring APDU traffic. Two toggles control visibility of
 * server (incoming) and NFC (outgoing) streams while the logs expand to
 * fill available space when shown individually.
 */
@Composable
fun CommunicationScreen(
    entries: List<CommunicationLog.Entry>,
    modifier: Modifier = Modifier
) {
    var showServer by rememberSaveable { mutableStateOf(true) }
    var showNfc by rememberSaveable { mutableStateOf(true) }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val segColors = SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.primary,
            activeContentColor = MaterialTheme.colorScheme.onPrimary
        )
        MultiChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("CommSegments")
        ) {
            SegmentedButton(
                checked = showServer,
                onCheckedChange = { showServer = it },
                enabled = showNfc || !showServer,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = segColors,
                modifier = Modifier.testTag("ServerToggle")
            ) { Text("Server") }
            SegmentedButton(
                checked = showNfc,
                onCheckedChange = { showNfc = it },
                enabled = showServer || !showNfc,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = segColors,
                modifier = Modifier.testTag("NfcToggle")
            ) { Text("NFC") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .testTag("ToggleDivider")
        )
        Spacer(modifier = Modifier.height(8.dp))

        val serverEntries = entries.filter { it.isRequest }
        val nfcEntries = entries.filter { !it.isRequest }

        if (showServer) {
            CommunicationLogList(
                label = "Server Communication",
                entries = serverEntries,
                tag = "ServerLog",
                modifier = Modifier.weight(1f)
            )
        }
        if (showServer && showNfc) {
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (showNfc) {
            CommunicationLogList(
                label = "NFC Communication",
                entries = nfcEntries,
                tag = "NfcLog",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val file = File(context.filesDir, "communication-log.txt")
                    CommunicationLog.saveToFile(file)
                },
                modifier = Modifier.weight(1f).testTag("SaveButton")
            ) {
                Text("Save Communication")
            }
            Button(
                onClick = { CommunicationLog.clear() },
                modifier = Modifier.weight(1f).testTag("ClearButton")
            ) {
                Text("Clear Communication")
            }
        }
    }
}

@Composable
fun ScenarioScreen(modifier: Modifier = Modifier) {
    var title by rememberSaveable { mutableStateOf("") }
    var command by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { value ->
                if (value.matches(Regex("[^\\\\/:*?\"<>|]*"))) {
                    title = value
                } else {
                    Toast.makeText(
                        context,
                        "Title contains invalid filename characters",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("TitleField"),
            label = { Text("Title") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = { value ->
                    if (value.matches(Regex("[0-9a-fA-F]*"))) {
                        command = value
                    } else {
                        Toast.makeText(
                            context,
                            "Command can contain only hex notation characters",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f).testTag("CommandField"),
                label = { Text("Command") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (command.length % 2 != 0) {
                        Toast.makeText(
                            context,
                            "Even amount of nibbles is required",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.testTag("CommandSend")
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send Command")
            }
            IconButton(
                onClick = { command = "" },
                modifier = Modifier.testTag("CommandClear")
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear Command")
            }
        }
    }
}

@Composable
fun ServerScreen(modifier: Modifier = Modifier) {
    var isExternal by rememberSaveable { mutableStateOf(true) }
    var ip by rememberSaveable { mutableStateOf("") }
    var pollingTime by rememberSaveable { mutableStateOf("") }
    var autoConnect by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val ipRegex =
        Regex("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$")

    val segColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary
    )

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ServerType")
        ) {
            SegmentedButton(
                selected = isExternal,
                onClick = { isExternal = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = segColors,
                modifier = Modifier.testTag("ExternalToggle")
            ) { Text("External") }
            SegmentedButton(
                selected = !isExternal,
                onClick = { isExternal = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = segColors,
                modifier = Modifier.testTag("InternalToggle")
            ) { Text("Internal") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = ip,
                onValueChange = { value ->
                    if (value.matches(Regex("[0-9.]*"))) {
                        ip = value
                    } else {
                        Toast.makeText(
                            context,
                            "IP can contain only digits and dots",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f).testTag("IpField"),
                label = { Text("Server IP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { },
                enabled = ipRegex.matches(ip),
                modifier = Modifier.testTag("IpApply")
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Apply IP")
            }
            IconButton(
                onClick = { ip = "" },
                modifier = Modifier.testTag("IpClear")
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear IP")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pollingTime,
            onValueChange = { value ->
                if (value.matches(Regex("\\d*"))) {
                    if (value.isEmpty() || value.toInt() <= 10000) {
                        pollingTime = value
                    } else {
                        Toast.makeText(
                            context,
                            "Max polling time is 10000",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Polling time can contain only digits",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            label = { Text("Polling Time [Ms]") },
            placeholder = { Text("Polling Time [Ms]") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = pollingTime.isNotEmpty() && pollingTime.toInt() < 10,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("PollingField")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleCheckbox(
                checked = autoConnect,
                onCheckedChange = { autoConnect = it },
                modifier = Modifier.testTag("AutoConnectCheck")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect Automatically")
        }
    }
}

@Composable
fun CircleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val borderColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val background = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(background)
            .border(2.dp, borderColor, CircleShape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Simple placeholder used for sections that are not yet implemented.
 */
@Composable
fun PlaceholderScreen(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Center) {
        Text(text)
    }
}

/**
 * Renders a labeled list of communication log [entries]. Each list item is
 * colored red for requests and green for responses.
 */
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

