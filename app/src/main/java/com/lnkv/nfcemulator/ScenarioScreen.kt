package com.lnkv.nfcemulator

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList

/**
 * Scenario list and editing UI extracted from MainActivity.
 */
@Composable
fun ScenarioScreen(modifier: Modifier = Modifier, onPlayScenario: () -> Unit = {}) {
    val context = LocalContext.current
    val scenarios = remember { loadScenarios(context) }
    val selected = remember { mutableStateListOf<Int>() }
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var deleteIndices by remember { mutableStateOf<List<Int>?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            exportScenarios(
                context,
                scenarios.filterIndexed { index, _ -> index in selected },
                uri,
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val imported = importScenarios(context, uri)
            scenarios.clear()
            scenarios.addAll(imported)
            selected.clear()
            saveScenarios(context, scenarios)
            Toast.makeText(context, "${imported.size} item(s) have been imported.", Toast.LENGTH_SHORT).show()
        }
    }

    if (editingIndex != null) {
        BackHandler { editingIndex = null }
        val isNew = editingIndex == -1
        val workingScenario = remember(editingIndex) {
            if (isNew) Scenario("", "") else {
                val original = scenarios[editingIndex!!]
                Scenario(
                    name = original.name,
                    aid = original.aid,
                    selectOnce = original.selectOnce,
                    steps = original.steps.toMutableStateList(),
                )
            }
        }
        ScenarioEditor(
            modifier = modifier,
            scenario = workingScenario,
            onSave = { updated ->
                val existingIndex = scenarios.indexOfFirst { it.name == updated.name }
                if (existingIndex >= 0) {
                    scenarios[existingIndex] = updated
                    if (!isNew && existingIndex != editingIndex) {
                        scenarios.removeAt(editingIndex!!)
                    }
                } else {
                    if (isNew) scenarios.add(updated) else scenarios[editingIndex!!] = updated
                }
                editingIndex = null
                selected.clear()
                saveScenarios(context, scenarios)
            },
            onCancel = { editingIndex = null },
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Scenarios",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { editingIndex = -1 },
                        modifier = Modifier.size(40.dp).testTag("ScenarioNew"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "New Scenario",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Button(
                        onClick = { showFilter = true },
                        modifier = Modifier.size(40.dp).testTag("ScenarioFilter"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Box {
                        Button(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Select All") },
                                onClick = {
                                    selected.clear()
                                    selected.addAll(scenarios.indices)
                                    showMenu = false
                                },
                            )
                            if (selected.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Deselect All") },
                                    onClick = {
                                        selected.clear()
                                        showMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        deleteIndices = selected.toList()
                                        showMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Export") },
                                    onClick = {
                                        exportLauncher.launch("scenarios.json")
                                        showMenu = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Import") },
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json"))
                                    showMenu = false
                                },
                            )
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("ScenarioList"),
            ) {
                val displayItems = scenarios.withIndex()
                    .filter { it.value.name.contains(filter, ignoreCase = true) }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(displayItems, key = { _, item -> item.index }) { idx, (index, scenario) ->
                        val isSelected = selected.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent,
                                )
                                .clickable {
                                    if (isSelected) selected.remove(index) else selected.add(index)
                                }
                                .padding(horizontal = 12.dp)
                                .testTag("ScenarioItem$index"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                scenario.name,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            ScenarioManager.setCurrent(context, scenario.name)
                                            onPlayScenario()
                                        },
                                        modifier = Modifier.size(40.dp).testTag("ScenarioPlay$index"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Play",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                    Button(
                                        onClick = { editingIndex = index },
                                        modifier = Modifier.size(40.dp).testTag("ScenarioEdit$index"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                    Button(
                                        onClick = { deleteIndices = listOf(index) },
                                        modifier = Modifier.size(40.dp).testTag("ScenarioDelete$index"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                        }
                        if (idx < displayItems.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        if (deleteIndices != null) {
            val count = deleteIndices!!.size
            val msg = "Do you really want to delete $count item(s)?"
            AlertDialog(
                onDismissRequest = { deleteIndices = null },
                confirmButton = {
                    Button(onClick = {
                        deleteIndices!!.sortedDescending().forEach { idx ->
                            val removed = scenarios.removeAt(idx)
                            if (ScenarioManager.current.value == removed.name) {
                                ScenarioManager.setCurrent(context, null)
                            }
                        }
                        selected.clear()
                        deleteIndices = null
                        saveScenarios(context, scenarios)
                    }) { Text("OK") }
                },
                dismissButton = {
                    Button(onClick = { deleteIndices = null }) { Text("Cancel") }
                },
                text = { Text(msg) },
            )
        }
        if (showFilter) {
            var temp by remember { mutableStateOf(filter) }
            AlertDialog(
                onDismissRequest = { showFilter = false },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            filter = ""
                            showFilter = false
                        }) { Text("Clear") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            filter = temp
                            showFilter = false
                        }) { Text("Apply") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFilter = false }) { Text("Cancel") }
                },
                text = {
                    OutlinedTextField(
                        value = temp,
                        onValueChange = { temp = it },
                        label = { Text("Filter") },
                    )
                },
            )
        }
    }
}

@Composable
fun ScenarioEditor(
    modifier: Modifier = Modifier,
    scenario: Scenario,
    onSave: (Scenario) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember { mutableStateOf(scenario.name) }
    val steps = scenario.steps
    var selectedStep by remember { mutableStateOf<Int?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    var showTitleAlert by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var aid by remember { mutableStateOf(scenario.aid) }
    var selectOnce by remember { mutableStateOf(scenario.selectOnce) }

    if (editingStepIndex != null) {
        val isNewStep = editingStepIndex == -1
        val workingStep = remember(editingStepIndex) {
            if (isNewStep) Step("") else steps[editingStepIndex!!].copy()
        }
        val otherNames = steps.mapIndexed { index, s -> index to s.name }
            .filter { it.first != editingStepIndex }
            .map { it.second }
        StepEditor(
            modifier = modifier,
            step = workingStep,
            existingNames = otherNames,
            onSave = { updated ->
                if (isNewStep) steps.add(updated) else steps[editingStepIndex!!] = updated
                editingStepIndex = null
                selectedStep = null
            },
            onCancel = { editingStepIndex = null },
        )
    } else {
        BackHandler(onBack = onCancel)
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { input ->
                    if (input.all { it.isLetterOrDigit() || it in listOf('_', '-', '.', ' ') }) title = input
                },
                label = { Text("Scenario Title") },
                trailingIcon = {
                    IconButton(onClick = { title = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear title")
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("ScenarioTitle"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = aid,
                onValueChange = { input ->
                    val filtered = input.uppercase()
                    if (filtered.length <= 32 && filtered.all { it in '0'..'9' || it in 'A'..'F' }) aid = filtered
                },
                label = { Text("AID") },
                singleLine = true,
                isError = aid.isNotEmpty() && !isValidAid(aid),
                modifier = Modifier.fillMaxWidth().testTag("ScenarioAid"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(
                    checked = selectOnce,
                    onCheckedChange = { selectOnce = it },
                    modifier = Modifier.testTag("ScenarioSelectOnce"),
                )
                Text("Select once")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("StepList"),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(steps) { index, step ->
                        val isSelected = selectedStep == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent,
                                )
                                .clickable { selectedStep = index }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("StepItem$index"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(step.name, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { editingStepIndex = index },
                                modifier = Modifier.testTag("StepEdit$index"),
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Step")
                            }
                            IconButton(
                                onClick = {
                                    steps.removeAt(index)
                                    if (selectedStep == index) selectedStep = null
                                },
                                modifier = Modifier.testTag("StepDelete$index"),
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Step")
                            }
                        }
                        if (index < steps.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { editingStepIndex = -1 },
                    modifier = Modifier.weight(1f).testTag("StepNew"),
                ) { Text("New Step") }
                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.weight(1f).testTag("StepClear"),
                ) { Text("Clear Steps") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            showTitleAlert = true
                        } else {
                            scenario.name = title
                            scenario.aid = aid
                            scenario.selectOnce = selectOnce
                            onSave(scenario)
                        }
                    },
                    enabled = isValidAid(aid),
                    modifier = Modifier.weight(1f).testTag("ScenarioSave"),
                ) { Text("Save") }
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).testTag("ScenarioCancel"),
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
                text = { Text("Clear all steps?") },
            )
        }
        if (showTitleAlert) {
            AlertDialog(
                onDismissRequest = { showTitleAlert = false },
                confirmButton = {
                    Button(onClick = { showTitleAlert = false }) { Text("OK") }
                },
                text = { Text("Title is required") },
            )
        }
    }
}

@Composable
fun StepEditor(
    modifier: Modifier = Modifier,
    step: Step,
    existingNames: List<String> = emptyList(),
    onSave: (Step) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(step.name) }
    var request by remember { mutableStateOf(step.request) }
    var response by remember { mutableStateOf(step.response) }
    val hexRegex = remember { Regex("^[0-9A-Fa-f]*$") }
    val reqValid = request.matches(hexRegex) && request.length % 2 == 0
    val respValid = response.matches(hexRegex) && response.length % 2 == 0
    val nameUnique = name.isNotBlank() && (name == step.name || !existingNames.contains(name))
    BackHandler(onBack = onCancel)
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { input ->
                if (input.all { it.isLetterOrDigit() || it in listOf('_', '-', '.', ' ') }) name = input
            },
            label = { Text("Step Name") },
            isError = name.isBlank() || (name != step.name && existingNames.contains(name)),
            trailingIcon = {
                IconButton(onClick = { name = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear step name")
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("StepName"),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = request,
            onValueChange = { input ->
                val filtered = input.uppercase()
                if (filtered.matches(hexRegex)) request = filtered
            },
            label = { Text("Request") },
            isError = request.isNotEmpty() && request.length % 2 != 0,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = response,
            onValueChange = { input ->
                val filtered = input.uppercase()
                if (filtered.matches(hexRegex)) response = filtered
            },
            label = { Text("Response") },
            isError = response.isNotEmpty() && response.length % 2 != 0,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    step.name = name
                    step.request = request
                    step.response = response
                    onSave(step)
                },
                enabled = reqValid && respValid && nameUnique,
                modifier = Modifier.weight(1f).testTag("StepSave"),
            ) { Text("Save") }
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag("StepCancel"),
            ) { Text("Cancel") }
        }
    }
}

