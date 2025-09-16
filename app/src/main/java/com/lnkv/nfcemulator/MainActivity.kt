@file:OptIn(ExperimentalMaterial3Api::class)

package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.ui.unit.sp
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.AnnotatedString
import android.content.res.ColorStateList
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.lnkv.nfcemulator.cardservice.TypeAEmulatorService
import com.lnkv.nfcemulator.ui.theme.NFCEmulatorTheme
import android.util.Log

/**
 * Main activity hosting a bottom navigation menu that switches between
 * Communication, Scenarios, and Server screens.
 */
class MainActivity : ComponentActivity() {
    private lateinit var cardEmulation: CardEmulation
    private lateinit var componentName: ComponentName
    private lateinit var prefs: SharedPreferences
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "onCreate")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Disable back button to keep the app in the foreground
            }
        })

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        cardEmulation = CardEmulation.getInstance(nfcAdapter)
        componentName = ComponentName(this, TypeAEmulatorService::class.java)
        prefs = getSharedPreferences("nfc_aids", MODE_PRIVATE)

        AppContextHolder.init(applicationContext)
        AidManager.init(cardEmulation, componentName, prefs)

        val storedAids = prefs.getStringSet("aids", setOf("F0010203040506"))!!.toList()
        Log.d(TAG, "storedAids: $storedAids")
        AidManager.registerAids(storedAids)

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
                    Log.d(TAG, "autoConnect to $host:$port poll=$poll")
                    ServerConnectionManager.connect(this, host, port, poll)
                }
            }
        }

        if (serverPrefs.getBoolean("autoStart", false) && getLocalIpAddress(this) != null) {
            val staticPort = serverPrefs.getBoolean("staticPort", false)
            val portStr = serverPrefs.getString("port", "0000")!!
            val portNum = if (staticPort && portStr.isNotBlank()) portStr.toIntOrNull() ?: 0 else 0
            if (ServerConnectionManager.state == "Connected") {
                Log.d(TAG, "disconnect before autoStart")
                ServerConnectionManager.disconnect()
            }
            Log.d(TAG, "autoStart server port=$portNum")
            InternalServerManager.start(portNum)
        }

        ScenarioManager.load(this)
        CommunicationFilter.load(this)
        Log.d(TAG, "initialization complete")

        setContent {
            NFCEmulatorTheme {
                MainScreen()
            }
        }
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
                                .defaultMinSize(minHeight = 48.dp)
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
    existingNames: List<String> = emptyList(),
    onSave: (Step) -> Unit,
    onCancel: () -> Unit
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
            modifier = Modifier.fillMaxWidth().testTag("StepName")
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
            modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    step.name = name
                    step.request = request
                    step.response = response
                    onSave(step)
                },
                enabled = reqValid && respValid && nameUnique,
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
    Communication("Comm"),
    Scenario("Scenarios"),
    Server("Server"),
    Aid("AID")
}

data class Step(
    var name: String,
    var request: String = "",
    var response: String = ""
)

data class Scenario(
    var name: String,
    var aid: String,
    var selectOnce: Boolean = false,
    val steps: SnapshotStateList<Step> = mutableStateListOf()
)

private const val SCENARIO_PREFS = "scenario_prefs"
private const val SCENARIO_KEY = "scenarios"

private fun saveScenarios(context: Context, scenarios: List<Scenario>) {
    val serialized = scenarios.map { scenario ->
        val stepString = scenario.steps.joinToString(",") { step ->
            listOf(
                step.name,
                step.request,
                step.response
            ).joinToString(";")
        }
        listOf(scenario.name, scenario.aid, scenario.selectOnce.toString()).joinToString(";") + "|" + stepString
    }.toSet()
    val prefs = context.getSharedPreferences(SCENARIO_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(SCENARIO_KEY, serialized).apply()
}

private fun loadScenarios(context: Context): SnapshotStateList<Scenario> {
    val prefs = context.getSharedPreferences(SCENARIO_PREFS, Context.MODE_PRIVATE)
    val serialized = prefs.getStringSet(SCENARIO_KEY, emptySet())!!
    return serialized.mapNotNull { line ->
        val parts = line.split("|", limit = 2)
        val header = parts[0].split(";", limit = 3)
        val name = header.getOrElse(0) { "" }
        if (name.isBlank()) return@mapNotNull null
        val aid = header.getOrElse(1) { "" }
        val selectOnce = header.getOrElse(2) { "false" }.toBoolean()
        val steps = if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1].split(",").mapNotNull { stepStr ->
                val sp = stepStr.split(";")
                val stepName = sp.getOrElse(0) { "" }
                if (stepName.isBlank()) return@mapNotNull null
                Step(
                    stepName,
                    sp.getOrElse(1) { "" },
                    sp.getOrElse(2) { "" }
                )
            }.associateBy { it.name }
                .values
                .toMutableStateList()
        } else mutableStateListOf()
        Scenario(name = name, aid = aid, selectOnce = selectOnce, steps = steps)
    }.toMutableStateList()
}

private fun exportScenarios(context: Context, scenarios: List<Scenario>, uri: Uri) {
    if (scenarios.isEmpty()) return
    val json = JSONArray().apply {
        scenarios.forEach { scenario ->
            put(
                JSONObject().apply {
                    put("name", scenario.name)
                    put("aid", scenario.aid)
                    put("selectOnce", scenario.selectOnce)
                    put("steps", JSONArray().apply {
                        scenario.steps.forEach { step ->
                            put(
                                JSONObject().apply {
                                    put("name", step.name)
                                    put("request", step.request)
                                    put("response", step.response)
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
        val name = obj.optString("name")
        if (name.isBlank()) continue
        val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
        val map = mutableMapOf<String, Step>()
        for (j in 0 until stepsArray.length()) {
            val stepObj = stepsArray.optJSONObject(j) ?: continue
            val stepName = stepObj.optString("name")
            if (stepName.isBlank() || map.containsKey(stepName)) continue
            map[stepName] = Step(
                stepName,
                stepObj.optString("request"),
                stepObj.optString("response")
            )
        }
        val selectOnce = obj.optBoolean("selectOnce", false)
        scenarios.removeAll { it.name == name }
        scenarios.add(Scenario(name, obj.optString("aid"), selectOnce, map.values.toMutableList().toMutableStateList()))
    }
    return scenarios
}

@OptIn(ExperimentalMaterial3Api::class)
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
                          if (isRunning) {
                              ScenarioManager.setRunning(false)
                          }
                          ScenarioManager.setCurrent(context, null)
                      },
                      onToggleSilence = {
                          ScenarioManager.toggleSilence()
                      },
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

/**
 * UI for monitoring APDU traffic. Two toggles control visibility of
 * server (incoming) and NFC (outgoing) streams while the logs expand to
 * fill available space when shown individually.
 */
@Composable
fun CommunicationScreen(
    entries: List<CommunicationLog.Entry>,
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
    val context = LocalContext.current
    var pendingSave by remember { mutableStateOf<Pair<String, List<CommunicationLog.Entry>>?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val pending = pendingSave
        if (uri != null && pending != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            val directory = DocumentFile.fromTreeUri(context, uri)
            if (directory != null) {
                val file = directory.createFile("text/plain", pending.first)
                if (file != null) {
                    try {
                        context.contentResolver.openOutputStream(file.uri)?.use { stream ->
                            CommunicationLog.saveToStream(stream, pending.second)
                        }
                        Toast.makeText(context, "Logs stored to file ${pending.first}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed to save logs: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(context, "Unable to create log file", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Unable to access selected folder", Toast.LENGTH_SHORT).show()
            }
        }
        pendingSave = null
    }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val scenarioName = (currentScenario ?: "log").replace(" ", "_")
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.getDefault()).format(java.util.Date())
                    val fileName = "${scenarioName}_${timestamp}.log"
                    pendingSave = fileName to filteredEntries.toList()
                    saveLauncher.launch(null)
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
        BackHandler { editingIndex = null }
        val isNew = editingIndex == -1
        val workingScenario = remember(editingIndex) {
            if (isNew) Scenario("", "") else {
                val original = scenarios[editingIndex!!]
                Scenario(
                    name = original.name,
                    aid = original.aid,
                    selectOnce = original.selectOnce,
                    steps = original.steps.toMutableStateList()
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { editingIndex = -1 },
                        modifier = Modifier.size(40.dp).testTag("ScenarioNew"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "New Scenario",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Button(
                        onClick = { showFilter = true },
                        modifier = Modifier.size(40.dp).testTag("ScenarioFilter"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Box {
                        Button(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
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
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
                    itemsIndexed(displayItems, key = { _, item -> item.index }) { idx, (index, scenario) ->
                        val isSelected = selected.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (isSelected) selected.remove(index) else selected.add(index)
                                }
                                .padding(horizontal = 12.dp)
                                .testTag("ScenarioItem$index"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                scenario.name,
                                modifier = Modifier.weight(1f)
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
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Play",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Button(
                                        onClick = { editingIndex = index },
                                        modifier = Modifier.size(40.dp).testTag("ScenarioEdit$index"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Button(
                                        onClick = { deleteIndices = listOf(index) },
                                        modifier = Modifier.size(40.dp).testTag("ScenarioDelete$index"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onPrimary
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
                text = { Text(msg) }
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
            onCancel = { editingStepIndex = null }
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
                modifier = Modifier.fillMaxWidth().testTag("ScenarioTitle")
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                modifier = Modifier.fillMaxWidth().testTag("ScenarioAid")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selectOnce,
                    onCheckedChange = { selectOnce = it },
                    modifier = Modifier.testTag("ScenarioSelectOnce")
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
                    .testTag("StepList")
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
                                    else Color.Transparent
                                )
                                .clickable { selectedStep = index }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("StepItem$index"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(step.name, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { editingStepIndex = index },
                                modifier = Modifier.testTag("StepEdit$index")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Step")
                            }
                            IconButton(
                                onClick = {
                                    steps.removeAt(index)
                                    if (selectedStep == index) selectedStep = null
                                },
                                modifier = Modifier.testTag("StepDelete$index")
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { editingStepIndex = -1 },
                    modifier = Modifier.weight(1f).testTag("StepNew")
                ) { Text("New Step") }
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
                            scenario.aid = aid
                            scenario.selectOnce = selectOnce
                            onSave(scenario)
                        }
                    },
                    enabled = isValidAid(aid),
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

private const val AID_PREFS = "nfc_aids"
private const val AID_KEY = "aids"

internal fun isValidAid(aid: String): Boolean {
    return aid.length in 10..32 && aid.length % 2 == 0 && aid.all { it in '0'..'9' || it in 'A'..'F' }
}

private fun loadAids(context: Context): SnapshotStateList<String> {
    val prefs = context.getSharedPreferences(AID_PREFS, Context.MODE_PRIVATE)
    val set = prefs.getStringSet(AID_KEY, setOf("F0010203040506")) ?: emptySet()
    return set.filter(::isValidAid).toMutableList().toMutableStateList()
}

private fun saveAids(context: Context, aids: List<String>) {
    val validAids = aids.filter(::isValidAid)
    if (validAids.size != aids.size) {
        Toast.makeText(context, "Some AIDs were invalid and ignored", Toast.LENGTH_SHORT).show()
    }
    val prefs = context.getSharedPreferences(AID_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(AID_KEY, validAids.toSet()).apply()
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    val cardEmulation = CardEmulation.getInstance(nfcAdapter)
    val component = ComponentName(context, TypeAEmulatorService::class.java)
    if (validAids.isEmpty()) {
        cardEmulation.removeAidsForService(component, CardEmulation.CATEGORY_OTHER)
    } else {
        cardEmulation.registerAidsForService(
            component,
            CardEmulation.CATEGORY_OTHER,
            validAids
        )
    }
}

private fun exportAids(context: Context, aids: List<String>, uri: Uri) {
    if (aids.isEmpty()) return
    context.contentResolver.openOutputStream(uri)?.use { os ->
        val arr = JSONArray()
        aids.forEach { arr.put(it) }
        os.write(arr.toString().toByteArray())
    }
    val name = uri.path?.substringAfterLast('/') ?: "aids.json"
    Toast.makeText(context, "AIDs saved to file: $name", Toast.LENGTH_SHORT).show()
}

private fun importAids(context: Context, uri: Uri): List<String> {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
        ?: return emptyList()
    val arr = JSONArray(text)
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) {
        val aid = arr.getString(i).uppercase()
        if (isValidAid(aid)) list.add(aid)
    }
    return list
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

@Composable
fun AidScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val aids = remember { loadAids(context) }
    var newAid by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            exportAids(context, aids, uri)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val imported = importAids(context, uri)
            aids.clear()
            aids.addAll(imported)
            saveAids(context, aids)
            Toast.makeText(context, "${imported.size} item(s) have been imported.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newAid,
                onValueChange = { input ->
                    val filtered = input.uppercase()
                    if (filtered.length <= 32 && filtered.all { it in '0'..'9' || it in 'A'..'F' }) newAid = filtered
                },
                label = { Text("New AID") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (isValidAid(newAid)) {
                        if (!aids.contains(newAid)) {
                            aids.add(newAid)
                            saveAids(context, aids)
                        }
                        newAid = ""
                    } else {
                        Toast.makeText(context, "Invalid AID", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Add") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { exportLauncher.launch("aids.json") }, modifier = Modifier.weight(1f)) {
                Text("Export")
            }
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) {
                Text("Import")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(aids) { index, aid ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(aid, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        val clipboard = LocalClipboardManager.current
                        IconButton(onClick = { clipboard.setText(AnnotatedString(aid)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy AID")
                        }
                        IconButton(onClick = {
                            aids.removeAt(index)
                            saveAids(context, aids)
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete AID")
                        }
                    }
                    if (index < aids.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
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
    val localIp = remember { getLocalIpAddress(context) }
    val ipRegex =
        Regex("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}:(\\d{1,5})$")

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
                label = { Text("External") },
                modifier = Modifier.testTag("ExternalToggle")
            )
            SegmentedButton(
                selected = !isExternal,
                onClick = {
                    isExternal = false
                    prefs.edit().putBoolean("isExternal", false).apply()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Internal") },
                modifier = Modifier.testTag("InternalToggle")
            )
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
            val externalStateColor = when {
                serverState.equals("Connected", ignoreCase = true) -> Color.Green
                serverState.contains("Disconnected", ignoreCase = true) ||
                    serverState.contains("Failed", ignoreCase = true) ||
                    serverState.contains("Stopped", ignoreCase = true) ||
                    serverState.contains("Error", ignoreCase = true) -> Color.Red
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                buildAnnotatedString {
                    append("Server State: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(serverState)
                    }
                },
                modifier = Modifier.testTag("ServerState"),
                color = externalStateColor
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
                                if (InternalServerManager.state == "Running") {
                                    InternalServerManager.stop()
                                }
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
            val internalStateColor = if (isServerRunning) Color.Green else Color.Red
            Text(
                buildAnnotatedString {
                    append("Server State: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(if (isServerRunning) "Running" else "Stopped")
                    }
                },
                modifier = Modifier.testTag("ServerState"),
                color = internalStateColor
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
            CommunicationFilter.setAll(imported, context)
            Toast.makeText(context, "${imported.size} item(s) have been imported.", Toast.LENGTH_SHORT).show()
        }
    }

    if (editingIndex != null) {
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            confirmButton = {
                TextButton(onClick = {
                    if (editingIndex == -1) {
                        CommunicationFilter.add(editInput, context)
                    } else {
                        CommunicationFilter.replace(filters[editingIndex!!], editInput, context)
                    }
                    editInput = ""
                    editingIndex = null
                }, enabled = editInput.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingIndex = null }) { Text("Cancel") } },
            text = {
                OutlinedTextField(
                    value = editInput,
                    onValueChange = {
                        if (it.uppercase().matches(Regex("[0-9A-F*]*"))) editInput = it.uppercase()
                    },
                    label = { Text("Filter") }
                )
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        editingIndex = -1
                        editInput = ""
                    },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Filter", tint = MaterialTheme.colorScheme.onPrimary)
                }
                Box {
                    Button(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Export") }, onClick = {
                            exportLauncher.launch("filters.json")
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text("Import") }, onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                            showMenu = false
                        })
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
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filters) { index, filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(filter, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            editingIndex = index
                            editInput = filter
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Filter")
                        }
                        IconButton(onClick = { CommunicationFilter.remove(filter, context) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Filter")
                        }
                    }
                    if (index < filters.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

