package com.lnkv.nfcemulator

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList

/**
 * Central store for the currently selected scenario and its execution state.
 */
object ScenarioManager {
    private const val PREFS = "scenario_prefs"
    private const val CURRENT_KEY = "currentScenario"
    private const val SCENARIO_KEY = "scenarios"
    private const val TAG = "ScenarioManager"

    private val SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    private val _current = MutableStateFlow<String?>(null)
    val current = _current.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()

    private val _silenced = MutableStateFlow(false)
    val silenced = _silenced.asStateFlow()

    private val _steps = MutableStateFlow<List<Step>>(emptyList())
    private var scenarioAid: String = ""
    private var aidToSelect: String = ""
    private var selectOnce: Boolean = false

    private var stepIndex = 0
    private var isSelected = false

    /**
     * Loads persisted scenario selection and metadata from preferences.
     *
     * @param context Context used to access shared preferences.
     */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(CURRENT_KEY, null)
        _current.value = name
        val scenario = if (name != null) loadScenario(context, name) else null
        _steps.value = scenario?.steps ?: emptyList()
        scenarioAid = scenario?.aid ?: ""
        aidToSelect = scenarioAid
        selectOnce = scenario?.selectOnce ?: false
        Log.d(TAG, "load: current=$name steps=${_steps.value.size} aid=$scenarioAid selectOnce=$selectOnce")
        if (name != null) {
            AppStatusManager.scenarioLoaded()
        }
    }

    /**
     * Sets the active scenario by name and updates dependent state.
     *
     * @param context Context used to read/write scenario preferences.
     * @param name Scenario name or `null` to clear selection.
     */
    fun setCurrent(context: Context, name: String?) {
        Log.d(TAG, "setCurrent: $name")
        _current.value = name
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(CURRENT_KEY, name).apply()
        val scenario = if (name != null) loadScenario(context, name) else null
        _steps.value = scenario?.steps ?: emptyList()
        scenarioAid = scenario?.aid ?: ""
        selectOnce = scenario?.selectOnce ?: false
        resetState()
        if (name != null) {
            AppStatusManager.scenarioLoaded()
            RequestStateTracker.markChanged()
        } else {
            AppStatusManager.scenarioUnloaded()
        }
    }

    /**
     * Starts or stops the currently selected scenario.
     *
     * @param running `true` to start execution, `false` to halt.
     */
    fun setRunning(running: Boolean) {
        Log.d(TAG, "setRunning: $running")
        _running.value = running
        val name = _current.value
        if (running && name != null) {
            CommunicationLog.add("STATE-SCEN: Scenario '$name' started.", true, true)
            if (scenarioAid.isNotEmpty()) {
                val onceText = if (selectOnce) "Yes" else "No"
                CommunicationLog.add("AID to Select: $scenarioAid (Select once: $onceText)", true)
            }
            AppStatusManager.scenarioRunning()
        } else if (!running && name != null) {
            val finished = _steps.value.isNotEmpty() && stepIndex >= _steps.value.size
            CommunicationLog.add("STATE-SCEN: Scenario '$name' stopped.", true, false)
            if (finished) {
                AppStatusManager.scenarioFinished()
            } else {
                AppStatusManager.scenarioStopped()
            }
            resetState()
        } else if (!running) {
            AppStatusManager.scenarioStopped()
            resetState()
        }
        if (running) {
            RequestStateTracker.markChanged()
        }
    }

    /**
     * Toggles whether scenario responses should be muted.
     */
    fun toggleSilence() {
        _silenced.value = !_silenced.value
        val name = _current.value?.let { "Scenario '$it'" } ?: "Scenario"
        if (_silenced.value) {
            CommunicationLog.add("STATE-SCEN: $name silenced.", true, false)
            AppStatusManager.silenced(true)
        } else {
            CommunicationLog.add("STATE-SCEN: $name unsilenced.", true, true)
            AppStatusManager.silenced(false)
        }
    }

    /**
     * Adds or updates the provided [scenario] within persistent storage.
     *
     * @param context Context used to access shared preferences.
     * @param scenario Scenario definition to persist.
     */
    fun addScenario(context: Context, scenario: Scenario) {
        if (scenario.name.isBlank()) return
        val uniqueSteps = scenario.steps
            .filter { it.name.isNotBlank() }
            .associateBy { it.name }
            .values
            .toMutableList()
            .toMutableStateList()
        val sanitized = scenario.copy(steps = uniqueSteps)
        val scenarios = loadAllScenarios(context)
        val index = scenarios.indexOfFirst { it.name == sanitized.name }
        if (index >= 0) {
            scenarios[index] = sanitized
        } else {
            scenarios.add(sanitized)
        }
        saveAllScenarios(context, scenarios)
        RequestStateTracker.markChanged()
    }

    /**
     * Removes the scenario identified by [name]. If it was active it is cleared.
     *
     * @param context Context used to update shared preferences.
     * @param name Scenario name to remove.
     */
    fun removeScenario(context: Context, name: String) {
        val scenarios = loadAllScenarios(context)
        scenarios.removeAll { it.name == name }
        saveAllScenarios(context, scenarios)
        if (_current.value == name) {
            setCurrent(context, null)
        }
        RequestStateTracker.markChanged()
    }

    /**
     * Removes all stored scenarios and clears the active selection.
     *
     * @param context Context used to clear shared preferences.
     */
    fun clearScenarios(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(SCENARIO_KEY).apply()
        setCurrent(context, null)
    }

    /**
     * Resets transient selection state when the NFC link is deactivated.
     */
    fun onDeactivated() {
        isSelected = false
        stepIndex = 0
    }

    /**
     * Processes the incoming [commandApdu] according to the running scenario.
     *
     * @param commandApdu Command from the reader.
     * @return Response bytes or `null` when silenced/inactive.
     */
    fun processApdu(commandApdu: ByteArray?): ByteArray? {
        if (commandApdu == null || _silenced.value || !_running.value || _current.value == null) return null

        val apduHex = commandApdu.toHex()
        Log.d(TAG, "processApdu: cmd=$apduHex index=$stepIndex selected=$isSelected aid=$aidToSelect")

        if (!isSelected) {
            if (isSelectCommand(commandApdu) && extractAid(commandApdu).equals(aidToSelect, true)) {
                isSelected = true
                if (selectOnce) aidToSelect = ""
                return SUCCESS
            }
            return FILE_NOT_FOUND
        }

        val step = _steps.value.getOrNull(stepIndex)
        if (step != null && apduHex.equals(step.request, true)) {
            stepIndex++
            val response = hexToBytes(step.response)
            if (stepIndex >= _steps.value.size) {
                CommunicationLog.add("STATE-SCEN: Scenario '${_current.value}' finished.", true, true)
                AppStatusManager.scenarioFinished()
                _running.value = false
                resetState()
                RequestStateTracker.markChanged()
            }
            return response
        }
        return SUCCESS
    }

    /**
     * Resets execution pointers and AID tracking for the next run.
     */
    private fun resetState() {
        stepIndex = 0
        isSelected = false
        aidToSelect = scenarioAid
    }

    /**
     * Loads a single scenario by [name] from shared preferences.
     *
     * @param context Context used to access shared preferences.
     * @param name Scenario identifier.
     * @return Parsed [Scenario] or `null` if not found.
     */
    private fun loadScenario(context: Context, name: String): Scenario? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val serialized = prefs.getStringSet(SCENARIO_KEY, emptySet()) ?: return null
        val line = serialized.find { it.startsWith("$name;") || it.startsWith("$name|") || it == name } ?: return null
        val parts = line.split("|", limit = 2)
        val header = parts[0].split(";", limit = 3)
        val aid = header.getOrElse(1) { "" }
        val selectOnce = header.getOrElse(2) { "false" }.toBoolean()
        val steps = if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1].split(",").mapNotNull { stepStr ->
                val sp = stepStr.split(";")
                if (sp.size < 3 || sp[0].isBlank()) return@mapNotNull null
                Step(sp[0], sp[1], sp[2])
            }.associateBy { it.name }
                .values
                .toMutableList()
                .toMutableStateList()
        } else mutableStateListOf()
        return Scenario(name, aid, selectOnce, steps)
    }

    /**
     * Loads all stored scenarios from shared preferences for bulk operations.
     *
     * @param context Context used to access shared preferences.
     * @return Mutable list of stored scenarios.
     */
    private fun loadAllScenarios(context: Context): MutableList<Scenario> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val serialized = prefs.getStringSet(SCENARIO_KEY, emptySet()) ?: emptySet()
        return serialized.mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            val header = parts[0].split(";", limit = 3)
            val name = header.getOrElse(0) { return@mapNotNull null }
            if (name.isBlank()) return@mapNotNull null
            val aid = header.getOrElse(1) { "" }
            val selectOnce = header.getOrElse(2) { "false" }.toBoolean()
            val steps = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].split(",").mapNotNull { stepStr ->
                    val sp = stepStr.split(";")
                    if (sp.size < 3 || sp[0].isBlank()) return@mapNotNull null
                    Step(sp[0], sp[1], sp[2])
                }.associateBy { it.name }
                    .values
                    .toMutableList()
                    .toMutableStateList()
            } else mutableStateListOf()
            Scenario(name, aid, selectOnce, steps)
        }.toMutableList()
    }

    /**
     * Persists the entire [scenarios] collection to shared preferences.
     *
     * @param context Context used to access shared preferences.
     * @param scenarios Scenario list to store.
     */
    private fun saveAllScenarios(context: Context, scenarios: List<Scenario>) {
        val serialized = scenarios.map { scenario ->
            val stepString = scenario.steps.joinToString(",") { step ->
                listOf(step.name, step.request, step.response).joinToString(";")
            }
            listOf(scenario.name, scenario.aid, scenario.selectOnce.toString()).joinToString(";") + "|" + stepString
        }.toSet()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(SCENARIO_KEY, serialized).apply()
    }

    /**
     * Determines whether the APDU is a SELECT command targeting an AID.
     *
     * @param apdu Command bytes received from the reader.
     * @return `true` when the command is a SELECT.
     */
    private fun isSelectCommand(apdu: ByteArray): Boolean {
        return apdu.size >= 4 &&
            apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x04.toByte()
    }

    /**
     * Extracts the AID from a SELECT APDU.
     *
     * @param apdu Command bytes received from the reader.
     * @return Uppercase hex representation of the AID.
     */
    private fun extractAid(apdu: ByteArray): String {
        if (apdu.size < 5) return ""
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return ""
        return apdu.copyOfRange(5, 5 + lc).toHex()
    }

    /**
     * Converts a hex string into a byte array.
     *
     * @param hex Hex string to convert.
     * @return Byte array decoded from the string.
     */
    private fun hexToBytes(hex: String): ByteArray {
        val cleaned = hex.uppercase()
        val result = ByteArray(cleaned.length / 2)
        for (i in result.indices) {
            val index = i * 2
            val byte = cleaned.substring(index, index + 2).toInt(16)
            result[i] = byte.toByte()
        }
        return result
    }
}

/**
 * Converts a byte array to an uppercase hex string.
 *
 * @receiver Byte array to convert.
 * @return Uppercase hex string representation.
 */
private fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it.toInt() and 0xFF) }
