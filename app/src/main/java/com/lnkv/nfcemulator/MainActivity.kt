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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import android.content.res.ColorStateList
import java.io.File
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
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

        val serverPrefs = getSharedPreferences("server_prefs", MODE_PRIVATE)
        if (serverPrefs.getBoolean("isExternal", true) &&
            serverPrefs.getBoolean("autoConnect", false)
        ) {
            val ipPort = serverPrefs.getString("ip", "0.0.0.0:0000")!!
            if (ipPort != "0.0.0.0:0000") {
                val host = ipPort.substringBefore(":")
                val port = ipPort.substringAfter(":").toIntOrNull()
                val poll = serverPrefs.getString("pollingTime", "0")!!.toLongOrNull() ?: 0
                if (port != null) {
                    ServerConnectionManager.connect(this, host, port, poll)
                }
            }
        }

        ScenarioManager.load(this)

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
    var delayMode by remember { mutableStateOf(step.delayMode) }
    var duration by remember {
        mutableStateOf(
            when (delayMode) {
                DelayMode.Duration -> step.durationMs.toString()
                DelayMode.Occurrences -> step.occurrences.toString()
                DelayMode.Always -> ""
            }
        )
    }
    val prevStepOptions = availableSteps.map { it.name }
    var previousStep by remember { mutableStateOf(step.previousStepName ?: prevStepOptions.firstOrNull()) }

    val triggerOptions = if (prevStepOptions.isEmpty()) {
        StepTrigger.entries.filter { it != StepTrigger.PreviousStep }
    } else StepTrigger.entries.toList()

    val hexRegex = remember { Regex("^[0-9A-Fa-f]*$") }
    val requestValid = trigger != StepTrigger.NfcRequest || (request.matches(hexRegex) && request.length % 2 == 0)
    val responseValid = action != StepAction.NfcResponse || (response.matches(hexRegex) && response.length % 2 == 0)
    val durationValid = delayMode == DelayMode.Always || duration.toIntOrNull()?.let { it in 1..1_000_000 } == true

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
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .testTag("StepNameDivider")
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
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = delayMode == DelayMode.Duration,
                onClick = {
                    delayMode = DelayMode.Duration
                    duration = step.durationMs.toString()
                },
                shape = SegmentedButtonDefaults.itemShape(0, 3)
            ) { Text("Duration") }
            SegmentedButton(
                selected = delayMode == DelayMode.Occurrences,
                onClick = {
                    delayMode = DelayMode.Occurrences
                    duration = step.occurrences.toString()
                },
                shape = SegmentedButtonDefaults.itemShape(1, 3)
            ) { Text("Occurrences") }
            SegmentedButton(
                selected = delayMode == DelayMode.Always,
                onClick = {
                    delayMode = DelayMode.Always
                    duration = ""
                },
                shape = SegmentedButtonDefaults.itemShape(2, 3)
            ) { Text("Always") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = duration,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                val value = filtered.toIntOrNull()
                duration = when {
                    filtered.isEmpty() -> ""
                    value == null -> duration
                    else -> value.coerceIn(1, 1_000_000).toString()
                }
            },
            label = {
                Text(
                    when (delayMode) {
                        DelayMode.Duration -> "Duration [ms]"
                        DelayMode.Occurrences -> "Times"
                        DelayMode.Always -> "Duration [ms]"
                    }
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("StepDuration"),
            enabled = delayMode != DelayMode.Always
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
                    step.delayMode = delayMode
                    when (delayMode) {
                        DelayMode.Duration -> step.durationMs = duration.toInt()
                        DelayMode.Occurrences -> step.occurrences = duration.toInt()
                        DelayMode.Always -> {
                            step.durationMs = 0
                            step.occurrences = 0
                        }
                    }
                    onSave(step)
                },
                enabled = requestValid && responseValid && durationValid,
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

enum class DelayMode { Duration, Occurrences, Always }

data class Step(
    var name: String,
    var trigger: StepTrigger = StepTrigger.ServerRequest,
    var action: StepAction = StepAction.ServerResponse,
    var request: String = "",
    var response: String = "",
    var previousStepName: String? = null,
    var durationMs: Int = 1000,
    var delayMode: DelayMode = DelayMode.Duration,
    var occurrences: Int = 1
)

data class Scenario(var name: String, val steps: SnapshotStateList<Step> = mutableStateListOf())

private const val SCENARIO_PREFS = "scenario_prefs"
private const val SCENARIO_KEY = "scenarios"

private fun saveScenarios(context: Context, scenarios: List<Scenario>) {
    val serialized = scenarios.map { scenario ->
        val stepString = scenario.steps.joinToString(",") { step ->
            listOf(
                step.name,
                step.trigger.name,
                step.action.name,
                step.request,
                step.response,
                step.previousStepName ?: "",
                step.durationMs.toString(),
                step.delayMode.name,
                step.occurrences.toString()
            ).joinToString(";")
        }
        scenario.name + "|" + stepString
    }.toSet()
    val prefs = context.getSharedPreferences(SCENARIO_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(SCENARIO_KEY, serialized).apply()
}

private fun loadScenarios(context: Context): SnapshotStateList<Scenario> {
    val prefs = context.getSharedPreferences(SCENARIO_PREFS, Context.MODE_PRIVATE)
    val serialized = prefs.getStringSet(SCENARIO_KEY, emptySet())!!
    return serialized.map { line ->
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
                val duration = sp.getOrElse(6) { "1000" }
                val mode = sp.getOrElse(7) { DelayMode.Duration.name }
                val occurrences = sp.getOrElse(8) { "1" }
                Step(
                    stepName,
                    StepTrigger.valueOf(trigger),
                    StepAction.valueOf(action),
                    request,
                    response,
                    prev.ifEmpty { null },
                    duration.toIntOrNull() ?: 1000,
                    DelayMode.valueOf(mode),
                    occurrences.toIntOrNull() ?: 1
                )
            }.toMutableStateList()
        } else mutableStateListOf()
        Scenario(name, steps)
    }.toMutableStateList()
}

private fun exportScenarios(context: Context, scenarios: List<Scenario>, uri: Uri) {
    if (scenarios.isEmpty()) return
    val json = JSONArray().apply {
        scenarios.forEach { scenario ->
            put(
                JSONObject().apply {
                    put("name", scenario.name)
                    put("steps", JSONArray().apply {
                        scenario.steps.forEach { step ->
                            put(
                                JSONObject().apply {
                                    put("name", step.name)
                                    put("trigger", step.trigger.name)
                                    put("action", step.action.name)
                                    put("request", step.request)
                                    put("response", step.response)
                                    put("previous", step.previousStepName ?: "")
                                    put("duration", step.durationMs)
                                    put("delayMode", step.delayMode.name)
                                    put("occurrences", step.occurrences)
                                }
                            )
                        }
                    })
                }
            )
        }
    }.toString()
    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    val name = uri.lastPathSegment ?: "file"
    Toast.makeText(context, "Scenarios saved to file: $name", Toast.LENGTH_SHORT).show()
}

private fun importScenarios(context: Context, uri: Uri): List<Scenario> {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return emptyList()
    val array = JSONArray(text)
    val scenarios = mutableListOf<Scenario>()
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
        val steps = mutableStateListOf<Step>()
        for (j in 0 until stepsArray.length()) {
            val stepObj = stepsArray.getJSONObject(j)
            steps.add(
                Step(
                    stepObj.getString("name"),
                    StepTrigger.valueOf(stepObj.getString("trigger")),
                    StepAction.valueOf(stepObj.getString("action")),
                    stepObj.getString("request"),
                    stepObj.getString("response"),
                    stepObj.optString("previous").ifBlank { null },
                    stepObj.optInt("duration", 1000),
                    DelayMode.valueOf(stepObj.getString("delayMode")),
                    stepObj.optInt("occurrences", 1)
                )
            )
        }
        scenarios.add(Scenario(obj.getString("name"), steps))
    }
    return scenarios
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Communication) }
    val logEntries by CommunicationLog.entries.collectAsState()
    val currentScenario by ScenarioManager.current.collectAsState()
    val context = LocalContext.current

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
                  CommunicationScreen(
                      logEntries,
                      currentScenario,
                      onClearScenario = { ScenarioManager.setCurrent(context, null) },
                      modifier = Modifier.padding(padding)
                  )
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
    currentScenario: String?,
    onClearScenario: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Current Scenario: ${currentScenario ?: "None"}",
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onClearScenario,
                enabled = currentScenario != null,
                modifier = Modifier.testTag("ScenarioClearButton")
            ) {
                Text("Clear")
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

        val serverEntries = entries.filter { it.isServer }
        val nfcEntries = entries.filter { !it.isServer }

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
                uri
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
                if (isNew) scenarios.add(updated) else scenarios[editingIndex!!] = updated
                editingIndex = null
                selected.clear()
                saveScenarios(context, scenarios)
            },
            onCancel = { editingIndex = null }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Scenarios",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = { editingIndex = -1 },
                    modifier = Modifier.testTag("ScenarioNew")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Scenario")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(onClick = { showFilter = true }, modifier = Modifier.testTag("ScenarioFilter")) {
                    Icon(Icons.Filled.Search, contentDescription = "Filter")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    FilledTonalIconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Select All") },
                            onClick = {
                                selected.clear()
                                selected.addAll(scenarios.indices)
                                showMenu = false
                            }
                        )
                        if (selected.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Deselect All") },
                                onClick = {
                                    selected.clear()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    deleteIndices = selected.toList()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export") },
                                onClick = {
                                    exportLauncher.launch("scenarios.json")
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Import") },
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                                showMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("ScenarioList")
            ) {
                val displayItems = scenarios.withIndex()
                    .filter { it.value.name.contains(filter, ignoreCase = true) }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayItems, key = { it.index }) { (index, scenario) ->
                        val isSelected = selected.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (isSelected) selected.remove(index) else selected.add(index)
                                }
                                .padding(12.dp)
                                .testTag("ScenarioItem$index"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                scenario.name,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                IconButton(
                                    onClick = {
                                        ScenarioManager.setCurrent(context, scenario.name)
                                    },
                                    modifier = Modifier.testTag("ScenarioPlay$index")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                                }
                                IconButton(
                                    onClick = { editingIndex = index },
                                    modifier = Modifier.testTag("ScenarioEdit$index")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(
                                    onClick = { deleteIndices = listOf(index) },
                                    modifier = Modifier.testTag("ScenarioDelete$index")
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
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
                text = { Text(msg) }
            )
        }
        if (showFilter) {
            var temp by remember { mutableStateOf(filter) }
            AlertDialog(
                onDismissRequest = { showFilter = false },
                confirmButton = {
                    Button(onClick = {
                        filter = temp
                        showFilter = false
                    }) { Text("Apply") }
                },
                dismissButton = {
                    Button(onClick = { showFilter = false }) { Text("Cancel") }
                },
                text = {
                    OutlinedTextField(
                        value = temp,
                        onValueChange = { temp = it },
                        label = { Text("Filter") }
                    )
                }
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
    var showTitleAlert by remember { mutableStateOf(false) }

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
                        if (title.isBlank()) {
                            showTitleAlert = true
                        } else {
                            scenario.name = title
                            onSave(scenario)
                        }
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
        if (showTitleAlert) {
            AlertDialog(
                onDismissRequest = { showTitleAlert = false },
                confirmButton = {
                    Button(onClick = { showTitleAlert = false }) { Text("OK") }
                },
                text = { Text("Title is required") }
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
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("server_prefs", Context.MODE_PRIVATE)
    var isExternal by rememberSaveable { mutableStateOf(prefs.getBoolean("isExternal", true)) }
    var ip by rememberSaveable { mutableStateOf(prefs.getString("ip", "0.0.0.0:0000")!!) }
    var pollingTime by rememberSaveable { mutableStateOf(prefs.getString("pollingTime", "0")!!) }
    var autoConnect by rememberSaveable { mutableStateOf(prefs.getBoolean("autoConnect", false)) }
    val serverState = ServerConnectionManager.state
    val isProcessing = ServerConnectionManager.isProcessing
    var port by rememberSaveable { mutableStateOf(prefs.getString("port", "0000")!!) }
    var staticPort by rememberSaveable { mutableStateOf(prefs.getBoolean("staticPort", false)) }
    var autoStart by rememberSaveable { mutableStateOf(prefs.getBoolean("autoStart", false)) }
    val internalState = InternalServerManager.state
    val isServerRunning = internalState == "Running"
    val connectedDevices = InternalServerManager.connectedDevices
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
                onClick = {
                    isExternal = true
                    prefs.edit().putBoolean("isExternal", true).apply()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = segColors,
                modifier = Modifier.testTag("ExternalToggle")
            ) { Text("External") }
            SegmentedButton(
                selected = !isExternal,
                onClick = {
                    isExternal = false
                    prefs.edit().putBoolean("isExternal", false).apply()
                },
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
                enabled = serverState != "Connected" && !isProcessing,
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
                enabled = serverState != "Connected" && !isProcessing,
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
                    onCheckedChange = {
                        autoConnect = it
                        prefs.edit().putBoolean("autoConnect", it).apply()
                    },
                    modifier = Modifier.testTag("AutoConnectCheck"),
                    enabled = serverState != "Connected" && !isProcessing
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Automatically")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("Server State: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(serverState)
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
                        prefs.edit()
                            .putBoolean("isExternal", isExternal)
                            .putString("ip", ip)
                            .putString("pollingTime", pollingTime)
                            .putBoolean("autoConnect", autoConnect)
                            .apply()
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    enabled = serverState != "Connected" && !isProcessing,
                    modifier = Modifier.weight(1f).testTag("SaveServer")
                ) {
                    Text("Save")
                }
                Button(
                    onClick = {
                        if (serverState == "Connected") {
                            ServerConnectionManager.disconnect()
                        } else if (ip.isBlank() || pollingTime.isBlank()) {
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
                                ServerConnectionManager.connect(
                                    context,
                                    ip.substringBefore(":"),
                                    portPart,
                                    pollingTime.toLong()
                                )
                            }
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f).testTag("ConnectButton")
                ) {
                    Text(if (serverState == "Connected") "Disconnect" else "Connect")
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
                        prefs.edit()
                            .putBoolean("isExternal", isExternal)
                            .putString("port", port)
                            .putBoolean("staticPort", staticPort)
                            .putBoolean("autoStart", autoStart)
                            .apply()
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isServerRunning,
                    modifier = Modifier.weight(1f).testTag("SaveServer")
                ) {
                    Text("Save")
                }
                Button(
                    onClick = {
                        if (isServerRunning) {
                            InternalServerManager.stop()
                        } else {
                            if (serverState == "Connected") {
                                ServerConnectionManager.disconnect()
                            }
                            val portNum = if (staticPort && port.isNotEmpty()) port.toInt() else 0
                            InternalServerManager.start(portNum)
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
                    val color = when (entry.isSuccess) {
                        true -> Color.Green
                        false -> Color.Red
                        null -> Color.Unspecified
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

