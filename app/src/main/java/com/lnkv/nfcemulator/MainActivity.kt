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
import androidx.compose.ui.draw.clip
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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import androidx.core.view.doOnLayout
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import android.content.res.ColorStateList
import java.io.File
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.lnkv.nfcemulator.cardservice.TypeAEmulatorService
import com.lnkv.nfcemulator.ui.theme.NFCEmulatorTheme

/**
 * Main activity hosting a bottom navigation menu that switches between
 * Communication, Scenarios, and Server screens.
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

@Composable

fun <T> EnumSpinner(
    label: String,
    options: List<T>,
    selected: T,
    labelMapper: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val labels = options.map(labelMapper)
    var selectedIndex by remember { mutableStateOf(options.indexOf(selected)) }

    fun dpToPx(dp: Float): Int = (dp * context.resources.displayMetrics.density).toInt()

    val adapter = remember(labels, colors) {
        object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(colors.onSurface.toArgb())
                tv.setPadding(dpToPx(16f), dpToPx(16f), dpToPx(16f), dpToPx(16f))
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                val isSelected = position == selectedIndex
                val bgColor = if (isSelected) colors.secondaryContainer.toArgb() else colors.surface.toArgb()
                val textColor = if (isSelected) colors.onSecondaryContainer.toArgb() else colors.onSurface.toArgb()
                tv.setTextColor(textColor)
                val drawable = GradientDrawable().apply {
                    setColor(bgColor)
                    setStroke(dpToPx(1f), colors.outline.toArgb())
                    cornerRadius = dpToPx(4f).toFloat()
                }
                tv.background = drawable
                tv.setPadding(dpToPx(16f), dpToPx(16f), dpToPx(16f), dpToPx(16f))
                tv.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            (v.background as GradientDrawable).setColor(colors.secondary.copy(alpha = 0.3f).toArgb())
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            (v.background as GradientDrawable).setColor(bgColor)
                        }
                    }
                    false
                }
                return tv
            }
        }
    }

    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.outline, RoundedCornerShape(4.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    Spinner(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        this.adapter = adapter
                        setSelection(selectedIndex)
                        backgroundTintList = ColorStateList.valueOf(colors.primary.toArgb())
                        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                selectedIndex = position
                                adapter.notifyDataSetChanged()
                                onSelected(options[position])
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                        val popupBg = GradientDrawable().apply {
                            setColor(colors.surface.toArgb())
                            setStroke(dpToPx(1f), colors.outline.toArgb())
                            cornerRadius = dpToPx(4f).toFloat()
                        }
                        setPopupBackgroundDrawable(popupBg)
                        doOnLayout { dropDownWidth = it.width }
                        isEnabled = enabled
                    }
                },
                update = { spinner ->
                    selectedIndex = options.indexOf(selected)
                    spinner.setSelection(selectedIndex)
                    spinner.dropDownWidth = spinner.width
                    spinner.backgroundTintList = ColorStateList.valueOf(colors.primary.toArgb())
                    spinner.isEnabled = enabled
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun StepEditor(
    modifier: Modifier = Modifier,
    step: Step,
    availableSteps: List<Step> = emptyList(),
    onSave: (Step) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(step.name) }
    var trigger by remember {
        mutableStateOf(
            if (availableSteps.isEmpty() && step.trigger == StepTrigger.PreviousStep)
                StepTrigger.ServerRequest else step.trigger
        )
    }
    var action by remember { mutableStateOf(step.action) }
    var request by remember { mutableStateOf(step.request) }
    var response by remember { mutableStateOf(step.response) }
    val prevStepOptions = availableSteps.map { it.name }
    var previousStep by remember { mutableStateOf(step.previousStepName ?: prevStepOptions.firstOrNull()) }

    val triggerOptions = if (prevStepOptions.isEmpty()) {
        StepTrigger.entries.filter { it != StepTrigger.PreviousStep }
    } else StepTrigger.entries.toList()

    val hexRegex = remember { Regex("^[0-9A-Fa-f]*$") }
    val requestValid = trigger != StepTrigger.NfcRequest || (request.matches(hexRegex) && request.length % 2 == 0)
    val responseValid = action != StepAction.NfcResponse || (response.matches(hexRegex) && response.length % 2 == 0)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { input ->
                if (input.all { it.isLetterOrDigit() || it in listOf('_', '-', '.') }) name = input
            },
            label = { Text("Step Name") },
            trailingIcon = {
                IconButton(onClick = { name = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear step name")
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("StepName")
        )
        Spacer(modifier = Modifier.height(8.dp))
        EnumSpinner(
            label = "Trigger",
            options = triggerOptions,
            selected = trigger,
            labelMapper = { it.label },
            onSelected = { trigger = it },
            modifier = Modifier.fillMaxWidth().testTag("TriggerSpinner")
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = request,
            onValueChange = { input ->
                val filtered = if (trigger == StepTrigger.NfcRequest) input.uppercase() else input
                if (trigger != StepTrigger.NfcRequest || filtered.matches(hexRegex)) request = filtered
            },
            label = { Text("Request") },
            isError = trigger == StepTrigger.NfcRequest && (request.isNotEmpty() && request.length % 2 != 0),
            modifier = Modifier.fillMaxWidth(),
            enabled = trigger in listOf(StepTrigger.ServerRequest, StepTrigger.NfcRequest)
        )
        if (prevStepOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            EnumSpinner(
                label = "Previous Step",
                options = prevStepOptions,
                selected = previousStep ?: prevStepOptions.first(),
                labelMapper = { it },
                onSelected = { previousStep = it },
                modifier = Modifier.fillMaxWidth().testTag("PrevStepSpinner"),
                enabled = trigger == StepTrigger.PreviousStep
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .testTag("StepOptionDivider")
        )
        Spacer(modifier = Modifier.height(8.dp))
        EnumSpinner(
            label = "Action",
            options = StepAction.entries.toList(),
            selected = action,
            labelMapper = { it.label },
            onSelected = { action = it },
            modifier = Modifier.fillMaxWidth().testTag("ActionSpinner")
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = response,
            onValueChange = { input ->
                val filtered = if (action == StepAction.NfcResponse) input.uppercase() else input
                if (action != StepAction.NfcResponse || filtered.matches(hexRegex)) response = filtered
            },
            label = { Text("Response") },
            isError = action == StepAction.NfcResponse && (response.isNotEmpty() && response.length % 2 != 0),
            modifier = Modifier.fillMaxWidth(),
            enabled = action in listOf(StepAction.ServerResponse, StepAction.NfcResponse)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    step.name = name
                    step.trigger = trigger
                    step.action = action
                    step.request = if (trigger in listOf(StepTrigger.ServerRequest, StepTrigger.NfcRequest)) request else ""
                    step.response = if (action in listOf(StepAction.ServerResponse, StepAction.NfcResponse)) response else ""
                    step.previousStepName = if (trigger == StepTrigger.PreviousStep) previousStep else null
                    onSave(step)
                },
                enabled = requestValid && responseValid,
                modifier = Modifier.weight(1f).testTag("StepSave")
            ) { Text("Save") }
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag("StepCancel")
            ) { Text("Cancel") }
        }
    }
}

/**
 * Navigation targets displayed in the bottom bar.
 */
enum class Screen(val label: String) {
    Communication("Communication"),
    Scenario("Scenarios"),
    Server("Server")
}

enum class StepTrigger(val label: String) {
    ServerRequest("Server Request"),
    NfcRequest("Nfc Request"),
    PreviousStep("Previous Step")
}

enum class StepAction(val label: String) {
    ServerResponse("Server Response"),
    NfcResponse("Nfc Response"),
    Silenced("Silenced")
}

data class Step(
    var name: String,
    var trigger: StepTrigger = StepTrigger.ServerRequest,
    var action: StepAction = StepAction.ServerResponse,
    var request: String = "",
    var response: String = "",
    var previousStepName: String? = null
)

data class Scenario(var name: String, val steps: SnapshotStateList<Step> = mutableStateListOf())

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
        save = { list ->
            list.map { scenario ->
                val stepString = scenario.steps.joinToString(",") { step ->
                    listOf(
                        step.name,
                        step.trigger.name,
                        step.action.name,
                        step.request,
                        step.response,
                        step.previousStepName ?: ""
                    ).joinToString(";")
                }
                scenario.name + "|" + stepString
            }
        },
        restore = { serialized ->
            serialized.map { line ->
                val parts = line.split("|", limit = 2)
                val name = parts[0]
                val steps = if (parts.size > 1 && parts[1].isNotEmpty()) {
                    parts[1].split(",").map { stepStr ->
                        val sp = stepStr.split(";")
                        val stepName = sp.getOrElse(0) { "" }
                        val trigger = sp.getOrElse(1) { StepTrigger.ServerRequest.name }
                        val action = sp.getOrElse(2) { StepAction.ServerResponse.name }
                        val request = sp.getOrElse(3) { "" }
                        val response = sp.getOrElse(4) { "" }
                        val prev = sp.getOrElse(5) { "" }
                        Step(
                            stepName,
                            StepTrigger.valueOf(trigger),
                            StepAction.valueOf(action),
                            request,
                            response,
                            prev.ifEmpty { null }
                        )
                    }.toMutableStateList()
                } else mutableStateListOf()
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
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }

    if (editingStepIndex != null) {
        val isNewStep = editingStepIndex == -1
        val workingStep = remember(editingStepIndex) {
            if (isNewStep) Step("") else steps[editingStepIndex!!].copy()
        }
        val available = remember(editingStepIndex, steps) {
            steps.filterIndexed { index, _ -> index != editingStepIndex }
        }
        StepEditor(
            modifier = modifier,
            step = workingStep,
            availableSteps = available,
            onSave = { updated ->
                if (isNewStep) steps.add(updated) else steps[editingStepIndex!!] = updated
                editingStepIndex = null
                selectedStep = null
            },
            onCancel = { editingStepIndex = null }
        )
    } else {
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
                            step.name,
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
                    onClick = { editingStepIndex = -1 },
                    modifier = Modifier.weight(1f).testTag("StepNew")
                ) { Text("New Step") }
                Button(
                    onClick = { editingStepIndex = selectedStep },
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

