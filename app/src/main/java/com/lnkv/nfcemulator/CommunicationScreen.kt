package com.lnkv.nfcemulator

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lnkv.nfcemulator.CommunicationLog.Entry
import java.io.File
import org.json.JSONArray

@Composable
fun CommunicationScreen(
    entries: List<Entry>,
    currentScenario: String?,
    isRunning: Boolean,
    isSilenced: Boolean,
    onToggleRun: () -> Unit,
    onClearScenario: () -> Unit,
    onToggleSilence: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showServer by rememberSaveable { mutableStateOf(true) }
    var showNfc by rememberSaveable { mutableStateOf(true) }
    var showFilterScreen by remember { mutableStateOf(false) }
    val filters by CommunicationFilter.filters.collectAsState()
    val filteredEntries = remember(entries, filters) {
        entries.filterNot { CommunicationFilter.shouldHide(it.message) }
    }
    val serverEntries = filteredEntries.filter { it.isServer }
    val nfcEntries = filteredEntries.filter { !it.isServer }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        MultiChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("CommSegments")
        ) {
            SegmentedButton(
                checked = showServer,
                onCheckedChange = { if (it || showNfc) showServer = it },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Server") },
                modifier = Modifier.testTag("ServerToggle")
            )
            SegmentedButton(
                checked = showNfc,
                onCheckedChange = { if (it || showServer) showNfc = it },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("NFC") },
                modifier = Modifier.testTag("NfcToggle")
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Current Scenario: ${currentScenario ?: "None"}",
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (currentScenario != null) {
                    Button(
                        onClick = onToggleRun,
                        modifier = Modifier.size(40.dp).testTag("ScenarioRunButton"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Stop" else "Run",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Button(
                        onClick = onClearScenario,
                        modifier = Modifier.size(40.dp).testTag("ScenarioClearButton"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Button(
                    onClick = onToggleSilence,
                    modifier = Modifier.size(40.dp).testTag("ScenarioSilenceButton"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (isSilenced) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = if (isSilenced) "Unsilence" else "Silence",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .testTag("ToggleDivider")
        )
        Spacer(modifier = Modifier.height(8.dp))
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
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val scenarioName = (currentScenario ?: "log").replace(" ", "_")
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.getDefault()).format(java.util.Date())
                    val fileName = "${scenarioName}_${timestamp}.log"
                    val file = File(context.filesDir, fileName)
                    CommunicationLog.saveToFile(file, filteredEntries)
                    Toast.makeText(context, "Logs stored to file $fileName", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).testTag("SaveButton")
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = "Save")
                Spacer(Modifier.width(4.dp))
                Text("Save")
            }
            Button(
                onClick = { showFilterScreen = !showFilterScreen },
                modifier = Modifier.weight(1f).testTag("FilterButton"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showFilterScreen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (showFilterScreen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Filters")
                Spacer(Modifier.width(4.dp))
                Text("Filters")
            }
            Button(
                onClick = { CommunicationLog.clear() },
                modifier = Modifier.weight(1f).testTag("ClearButton")
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear")
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }
        }
        if (showFilterScreen) {
            BackHandler { showFilterScreen = false }
            FilterScreen()
        }
    }
}

@Composable
private fun CommunicationLogList(
    label: String,
    entries: List<Entry>,
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
                    val color = when {
                        entry.message.startsWith("REQ:") -> Color.Yellow
                        entry.message.startsWith("RESP:") -> Color.Cyan
                        entry.message.uppercase().startsWith("AID TO SELECT:") -> Color(0xFFFF9800)
                        entry.isSuccess == true -> Color.Green
                        entry.isSuccess == false -> Color.Red
                        else -> Color.Unspecified
                    }
                    val time = remember(entry.timestamp) {
                        java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                            .format(java.util.Date(entry.timestamp))
                    }
                    Text("[$time] ${entry.message}", color = color)
                }
            }
        }
    }
}

@Composable
private fun FilterScreen() {
    val context = LocalContext.current
    val filters by CommunicationFilter.filters.collectAsState()
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var editInput by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) exportFilters(context, filters, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val imported = importFilters(context, uri)
            CommunicationFilter.setFilters(imported)
            Toast.makeText(context, "${imported.size} item(s) have been imported.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { showMenu = true }, modifier = Modifier.testTag("FilterMenu")) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Export") },
                    onClick = {
                        exportLauncher.launch("filters.json")
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import") },
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                        showMenu = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(filters) { index, filter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            editingIndex = index
                            editInput = filter
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(filter, modifier = Modifier.weight(1f))
                    IconButton(onClick = { CommunicationFilter.remove(filter) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Filter")
                    }
                }
                if (index < filters.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
        OutlinedTextField(
            value = editInput,
            onValueChange = {
                editInput = it.uppercase()
            },
            label = { Text("Filter") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                editingIndex = null
                editInput = ""
            }) { Text("Cancel") }
            TextButton(onClick = {
                if (editInput.matches(Regex("[0-9A-F*]+"))) {
                    if (editingIndex == null) {
                        CommunicationFilter.add(editInput)
                    } else {
                        CommunicationFilter.update(editingIndex!!, editInput)
                    }
                    editingIndex = null
                    editInput = ""
                } else {
                    Toast.makeText(context, "Invalid filter", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Save") }
        }
    }
}

private fun exportFilters(context: Context, filters: List<String>, uri: Uri) {
    if (filters.isEmpty()) return
    context.contentResolver.openOutputStream(uri)?.use { os ->
        val arr = JSONArray()
        filters.forEach { arr.put(it) }
        os.write(arr.toString().toByteArray())
    }
    val name = uri.path?.substringAfterLast('/') ?: "filters.json"
    Toast.makeText(context, "Filters saved to file: $name", Toast.LENGTH_SHORT).show()
}

private fun importFilters(context: Context, uri: Uri): List<String> {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
        ?: return emptyList()
    val arr = JSONArray(text)
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) {
        val filter = arr.getString(i).uppercase()
        if (filter.matches(Regex("[0-9A-F*]+"))) list.add(filter)
    }
    return list
}
