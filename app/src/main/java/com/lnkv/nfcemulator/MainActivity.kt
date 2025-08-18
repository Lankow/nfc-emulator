@file:OptIn(ExperimentalMaterial3Api::class)

package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.io.File
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

data class Scenario(var name: String, val steps: SnapshotStateList<String> = mutableStateListOf())

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
    val scenariosSaver = Saver<SnapshotStateList<Scenario>, List<String>>(
        save = { list -> list.map { it.name + "|" + it.steps.joinToString(",") } },
        restore = { serialized ->
            serialized.map { line ->
                val parts = line.split("|", limit = 2)
                val name = parts[0]
                val steps = if (parts.size > 1 && parts[1].isNotEmpty())
                    parts[1].split(",").toMutableStateList()
                else mutableStateListOf()
                Scenario(name, steps)
            }.toMutableStateList()
        }
    )
    val scenarios = rememberSaveable(saver = scenariosSaver) {
        mutableStateListOf(
            Scenario("Scenario 1"),
            Scenario("Scenario 2")
        )
    }
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (editingIndex != null) {
        val isNew = editingIndex == -1
        val workingScenario = remember(editingIndex) {
            if (isNew) Scenario("") else {
                val original = scenarios[editingIndex!!]
                Scenario(original.name, original.steps.toMutableStateList())
            }
        }
        ScenarioEditor(
            modifier = modifier,
            scenario = workingScenario,
            onSave = { updated ->
                if (isNew) scenarios.add(updated)
                else scenarios[editingIndex!!] = updated
                editingIndex = null
                selectedIndex = null
            },
            onCancel = { editingIndex = null }
        )
    } else {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("ScenarioList")
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(scenarios) { index, scenario ->
                        val isSelected = selectedIndex == index
                        Text(
                            scenario.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable { selectedIndex = index }
                                .padding(12.dp)
                                .testTag("ScenarioItem$index")
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { editingIndex = -1 },
                    modifier = Modifier.weight(1f).testTag("ScenarioNew")
                ) { Text("New") }
                Button(
                    onClick = { editingIndex = selectedIndex },
                    enabled = selectedIndex != null,
                    modifier = Modifier.weight(1f).testTag("ScenarioEdit")
                ) { Text("Edit") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* TODO save scenarios */ },
                    modifier = Modifier.weight(1f).testTag("ScenarioSave")
                ) { Text("Save") }
                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.weight(1f).testTag("ScenarioClear")
                ) { Text("Clear") }
            }
        }
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                confirmButton = {
                    Button(onClick = {
                        scenarios.clear()
                        selectedIndex = null
                        showClearDialog = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    Button(onClick = { showClearDialog = false }) { Text("Cancel") }
                },
                text = { Text("Clear all scenarios?") }
            )
        }
    }
}

@Composable
fun ScenarioEditor(
    modifier: Modifier = Modifier,
    scenario: Scenario,
    onSave: (Scenario) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(scenario.name) }
    val steps = scenario.steps
    var selectedStep by remember { mutableStateOf<Int?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var editStepIndex by remember { mutableStateOf<Int?>(null) }
    var stepText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { input ->
                if (input.all { it.isLetterOrDigit() || it in listOf('_', '-', '.') }) title = input
            },
            label = { Text("Scenario Title") },
            trailingIcon = {
                IconButton(onClick = { title = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear title")
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("ScenarioTitle")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("StepList")
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(steps) { index, step ->
                    val isSelected = selectedStep == index
                    Text(
                        step,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable { selectedStep = index }
                            .padding(12.dp)
                            .testTag("StepItem$index")
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { steps.add("Step ${steps.size + 1}") },
                modifier = Modifier.weight(1f).testTag("StepNew")
            ) { Text("New Step") }
            Button(
                onClick = {
                    editStepIndex = selectedStep
                    if (editStepIndex != null) stepText = steps[editStepIndex!!]
                },
                enabled = selectedStep != null,
                modifier = Modifier.weight(1f).testTag("StepEdit")
            ) { Text("Edit Step") }
            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.weight(1f).testTag("StepClear")
            ) { Text("Clear Steps") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scenario.name = title
                    onSave(scenario)
                },
                modifier = Modifier.weight(1f).testTag("ScenarioSave")
            ) { Text("Save") }
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag("ScenarioCancel")
            ) { Text("Cancel") }
        }
    }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            confirmButton = {
                Button(onClick = {
                    steps.clear()
                    selectedStep = null
                    showClearDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
            text = { Text("Clear all steps?") }
        )
    }
    if (editStepIndex != null) {
        AlertDialog(
            onDismissRequest = { editStepIndex = null },
            confirmButton = {
                Button(onClick = {
                    steps[editStepIndex!!] = stepText
                    editStepIndex = null
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { editStepIndex = null }) { Text("Cancel") }
            },
            text = {
                OutlinedTextField(
                    value = stepText,
                    onValueChange = { stepText = it },
                    label = { Text("Step") }
                )
            }
        )
    }
}

private fun getLocalIpAddress(context: Context): String? {
    return try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wm.connectionInfo?.ipAddress ?: return null
        if (ipInt == 0) null else {
            val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
            InetAddress.getByAddress(bytes).hostAddress
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ServerScreen(modifier: Modifier = Modifier) {
    var isExternal by rememberSaveable { mutableStateOf(true) }
    var ip by rememberSaveable { mutableStateOf("192.168.0.1:8080") }
    var pollingTime by rememberSaveable { mutableStateOf("100") }
    var autoConnect by rememberSaveable { mutableStateOf(false) }
    var isServerConnected by rememberSaveable { mutableStateOf(false) }
    var port by rememberSaveable { mutableStateOf("8080") }
    var staticPort by rememberSaveable { mutableStateOf(false) }
    var autoStart by rememberSaveable { mutableStateOf(false) }
    var isServerRunning by rememberSaveable { mutableStateOf(false) }
    val connectedDevices = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    val localIp = remember { getLocalIpAddress(context) }
    val ipRegex =
        Regex("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}:(\\d{1,5})$")

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
        if (isExternal) {
            OutlinedTextField(
                value = ip,
                onValueChange = { value ->
                    if (value.matches(Regex("[0-9.:]*"))) {
                        ip = value
                    } else {
                        Toast.makeText(
                            context,
                            "IP can contain only digits, dots and colon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("IpField"),
                label = { Text("Server IP:Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                enabled = !isServerConnected,
                trailingIcon = {
                    IconButton(onClick = { ip = "" }, modifier = Modifier.testTag("IpClear")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear IP")
                    }
                }
            )
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
                modifier = Modifier.fillMaxWidth().testTag("PollingField"),
                enabled = !isServerConnected,
                trailingIcon = {
                    IconButton(onClick = { pollingTime = "" }, modifier = Modifier.testTag("PollingClear")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear Polling Time")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleCheckbox(
                    checked = autoConnect,
                    onCheckedChange = { autoConnect = it },
                    modifier = Modifier.testTag("AutoConnectCheck"),
                    enabled = !isServerConnected
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Automatically")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("Server State: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(if (isServerConnected) "Connected" else "Disconnected")
                    }
                },
                modifier = Modifier.testTag("ServerState")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isServerConnected,
                    modifier = Modifier.weight(1f).testTag("SaveServer")
                ) {
                    Text("Save")
                }
                Button(
                    onClick = {
                        if (ip.isBlank() || pollingTime.isBlank()) {
                            Toast.makeText(
                                context,
                                "Server IP and Polling Time must be provided",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (!ipRegex.matches(ip)) {
                            Toast.makeText(
                                context,
                                "Invalid IP address or port",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val portPart = ip.substringAfter(":").toInt()
                            if (portPart !in 1..65535) {
                                Toast.makeText(
                                    context,
                                    "Invalid port",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                isServerConnected = !isServerConnected
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("ConnectButton")
                ) {
                    Text(if (isServerConnected) "Disconnect" else "Connect")
                }
            }
        } else {
            Text(
                "Server IP: ${localIp ?: "Unavailable"}",
                modifier = Modifier.testTag("InternalIp")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleCheckbox(
                    checked = staticPort,
                    onCheckedChange = { staticPort = it },
                    modifier = Modifier.testTag("StaticPortCheck"),
                    enabled = !isServerRunning
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use static port")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { value ->
                    if (value.matches(Regex("\\d*"))) {
                        port = value
                    } else {
                        Toast.makeText(
                            context,
                            "Port can contain only digits",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = staticPort && !isServerRunning,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("PortField"),
                trailingIcon = {
                    IconButton(onClick = { port = "" }, modifier = Modifier.testTag("PortClear")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear Port")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleCheckbox(
                    checked = autoStart,
                    onCheckedChange = { autoStart = it },
                    modifier = Modifier.testTag("AutoStartCheck"),
                    enabled = !isServerRunning
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Automatically")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("Server State: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(if (isServerRunning) "Running" else "Stopped")
                    }
                },
                modifier = Modifier.testTag("ServerState")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isServerRunning,
                    modifier = Modifier.weight(1f).testTag("SaveServer")
                ) {
                    Text("Save")
                }
                Button(
                    onClick = {
                        isServerRunning = !isServerRunning
                        if (isServerRunning) {
                            connectedDevices.clear()
                            connectedDevices.addAll(listOf("Device1", "Device2"))
                        } else {
                            connectedDevices.clear()
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("StartButton")
                ) {
                    Text(if (isServerRunning) "Close" else "Start")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connected devices (${connectedDevices.size})")
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("ConnectedList")
                    .padding(8.dp)
            ) {
                if (connectedDevices.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(connectedDevices) { device ->
                            Text(device)
                        }
                    }
                }
            }
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

