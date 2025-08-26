package com.lnkv.nfcemulator

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList

/**
 * Central store for the currently selected scenario and its execution state.
 * It exposes flows for the active scenario name, whether it is running, and a
 * global silence toggle that forces the emulator to return null responses.
 */
object ScenarioManager {
    private const val PREFS = "scenario_prefs"
    private const val CURRENT_KEY = "currentScenario"
    private const val SCENARIO_KEY = "scenarios"

    private val _current = MutableStateFlow<String?>(null)
    val current = _current.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()

    private val _silenced = MutableStateFlow(false)
    val silenced = _silenced.asStateFlow()

    private val _steps = MutableStateFlow<List<Step>>(emptyList())

    private var stepIndex = 0
    private var isSelected = false
    private val singleConsumed = mutableSetOf<String>()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(CURRENT_KEY, null)
        _current.value = name
        _steps.value = if (name != null) loadScenario(context, name)?.steps ?: emptyList() else emptyList()
    }

    fun setCurrent(context: Context, name: String?) {
        _current.value = name
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(CURRENT_KEY, name).apply()
        _steps.value = if (name != null) loadScenario(context, name)?.steps ?: emptyList() else emptyList()
        resetState()
    }

    fun setRunning(running: Boolean) {
        _running.value = running
        val name = _current.value
        if (running && name != null) {
            CommunicationLog.add("STATE-SCEN: Scenario '$name' started.", true, true)
        } else if (!running && name != null) {
            CommunicationLog.add("STATE-SCEN: Scenario '$name' stopped.", true, false)
            resetState()
        } else if (!running) {
            resetState()
        }
    }

    fun toggleSilence() {
        _silenced.value = !_silenced.value
        val name = _current.value?.let { "Scenario '$it'" } ?: "Scenario"
        if (_silenced.value) {
            CommunicationLog.add("STATE-SCEN: $name silenced.", true, false)
        } else {
            CommunicationLog.add("STATE-SCEN: $name unsilenced.", true, true)
        }
    }

    fun addScenario(context: Context, scenario: Scenario) {
        val scenarios = loadAllScenarios(context)
        scenarios.removeAll { it.name == scenario.name }
        scenarios.add(scenario)
        saveAllScenarios(context, scenarios)
    }

    fun removeScenario(context: Context, name: String) {
        val scenarios = loadAllScenarios(context)
        scenarios.removeAll { it.name == name }
        saveAllScenarios(context, scenarios)
        if (_current.value == name) {
            setCurrent(context, null)
        }
    }

    fun clearScenarios(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(SCENARIO_KEY).apply()
        setCurrent(context, null)
    }

    fun onDeactivated() {
        isSelected = false
    }

    fun processApdu(commandApdu: ByteArray?): ByteArray? {
        if (commandApdu == null) return null
        if (_silenced.value) return null
        if (!_running.value || _current.value == null) return null

        // Ignore further selects for single-select AIDs
        if (isSelectCommand(commandApdu)) {
            val aid = extractAid(commandApdu)
            if (singleConsumed.contains(aid)) return null
        }

        val steps = _steps.value
        val step = steps.getOrNull(stepIndex)
        if (step != null) {
            when (step.type) {
                StepType.Select -> {
                    if (isSelectCommand(commandApdu) && extractAid(commandApdu).equals(step.aid, true)) {
                        isSelected = true
                        if (step.singleSelect) singleConsumed.add(step.aid.uppercase())
                        stepIndex++
                        return byteArrayOf(0x90.toByte(), 0x00.toByte())
                    }
                    return null
                }
                StepType.RequestResponse -> {
                    val reqHex = commandApdu.toHex()
                    if (step.needsSelection == isSelected && reqHex.equals(step.request, true)) {
                        stepIndex++
                        return hexToBytes(step.response)
                    }
                }
            }
        }

        return if (isSelected) SettingsManager.selectedResponse.value.data else SettingsManager.unselectedResponse.value.data
    }

    private fun resetState() {
        stepIndex = 0
        isSelected = false
        singleConsumed.clear()
    }

    private fun loadScenario(context: Context, name: String): Scenario? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val serialized = prefs.getStringSet(SCENARIO_KEY, emptySet()) ?: return null
        val line = serialized.find { it.startsWith("$name|") || it == name } ?: return null
        val parts = line.split("|", limit = 2)
        val stepString = parts.getOrNull(1) ?: return Scenario(name)
        val steps = if (stepString.isNotEmpty()) {
            stepString.split(",").mapNotNull { stepStr ->
                val sp = stepStr.split(";")
                if (sp.size < 7) return@mapNotNull null
                Step(
                    sp[0],
                    StepType.valueOf(sp[1]),
                    sp[2],
                    sp[3].toBoolean(),
                    sp[4],
                    sp[5],
                    sp[6].toBoolean()
                )
            }
        } else emptyList()
        return Scenario(name, steps.toMutableList().toMutableStateList())
    }

    private fun loadAllScenarios(context: Context): MutableList<Scenario> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val serialized = prefs.getStringSet(SCENARIO_KEY, emptySet()) ?: emptySet()
        return serialized.map { line ->
            val parts = line.split("|", limit = 2)
            val name = parts[0]
            val steps = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].split(",").mapNotNull { stepStr ->
                    val sp = stepStr.split(";")
                    if (sp.size < 7) return@mapNotNull null
                    Step(
                        sp[0],
                        StepType.valueOf(sp[1]),
                        sp[2],
                        sp[3].toBoolean(),
                        sp[4],
                        sp[5],
                        sp[6].toBoolean()
                    )
                }.toMutableList().toMutableStateList()
            } else mutableStateListOf()
            Scenario(name, steps)
        }.toMutableList()
    }

    private fun saveAllScenarios(context: Context, scenarios: List<Scenario>) {
        val serialized = scenarios.map { scenario ->
            val stepString = scenario.steps.joinToString(",") { step ->
                listOf(
                    step.name,
                    step.type.name,
                    step.aid,
                    step.singleSelect.toString(),
                    step.request,
                    step.response,
                    step.needsSelection.toString()
                ).joinToString(";")
            }
            scenario.name + "|" + stepString
        }.toSet()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(SCENARIO_KEY, serialized).apply()
    }

    private fun isSelectCommand(apdu: ByteArray): Boolean {
        return apdu.size >= 4 &&
            apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x04.toByte()
    }

    private fun extractAid(apdu: ByteArray): String {
        if (apdu.size < 5) return ""
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return ""
        return apdu.copyOfRange(5, 5 + lc).toHex()
    }

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

private fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it.toInt() and 0xFF) }

