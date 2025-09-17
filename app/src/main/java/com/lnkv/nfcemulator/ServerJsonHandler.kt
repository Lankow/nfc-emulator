package com.lnkv.nfcemulator

import androidx.compose.runtime.toMutableStateList
import org.json.JSONObject
import android.util.Log

object ServerJsonHandler {
    private const val TAG = "ServerJsonHandler"

    /**
     * Parses [jsonStr] and applies the contained commands.
     * @return true if the communication log was cleared as part of the request.
     */
    fun handle(jsonStr: String): Boolean {
        Log.d(TAG, "handle: $jsonStr")
        var cleared = false
        try {
            val obj = JSONObject(jsonStr)

            val type = obj.optString("Type")
            if (type.isNotBlank()) {
                cleared = when (type) {
                    "Aid" -> { handleAid(obj); false }
                    "Comm" -> handleComm(obj)
                    "Scenarios" -> { handleScenarios(obj); false }
                    "Filters" -> { handleFilters(obj); false }
                    "Reset" -> { handleReset(); false }
                    else -> false
                }
                return cleared
            }

            obj.optJSONObject("Aid")?.let { handleAid(it) }
            obj.optJSONObject("Comm")?.let { if (handleComm(it)) cleared = true }
            obj.optJSONObject("Scenarios")?.let { handleScenarios(it) }
            obj.optJSONObject("Filters")?.let { handleFilters(it) }
            if (obj.has("Reset")) handleReset()
        } catch (e: Exception) {
            Log.d(TAG, "parse error: ${e.message}")
            CommunicationLog.add("JSON ERR: ${e.message}", true, false)
        }
        return cleared
    }

    private fun handleAid(obj: JSONObject) {
        parseFlexibleBoolean(obj.opt("Enabled"))?.let { enabled ->
            Log.d(TAG, "handleAid: enabled=$enabled")
            AidManager.setEnabled(enabled)
        }
        val clear = obj.optBoolean("Clear", false)
        if (clear) {
            Log.d(TAG, "handleAid: clear")
            AidManager.clear()
        }

        obj.opt("Add")?.let { addVal ->
            when (addVal) {
                is String -> if (addVal.isNotBlank()) {
                    Log.d(TAG, "handleAid: add $addVal")
                    AidManager.add(addVal)
                }
                is org.json.JSONArray -> {
                    for (i in 0 until addVal.length()) {
                        val aid = addVal.optString(i)
                        if (aid.isNotBlank()) {
                            Log.d(TAG, "handleAid: add $aid")
                            AidManager.add(aid)
                        }
                    }
                }
            }
        }

        obj.opt("Remove")?.let { removeVal ->
            when (removeVal) {
                is String -> if (removeVal.isNotBlank()) {
                    Log.d(TAG, "handleAid: remove $removeVal")
                    AidManager.remove(removeVal)
                }
                is org.json.JSONArray -> {
                    for (i in 0 until removeVal.length()) {
                        val aid = removeVal.optString(i)
                        if (aid.isNotBlank()) {
                            Log.d(TAG, "handleAid: remove $aid")
                            AidManager.remove(aid)
                        }
                    }
                }
            }
        }
    }

    private fun handleFilters(obj: JSONObject) {
        val context = AppContextHolder.context
        if (obj.optBoolean("Clear", false)) {
            Log.d(TAG, "handleFilters: clear")
            CommunicationFilter.clear(context)
        }

        obj.opt("Add")?.let { addVal ->
            when (addVal) {
                is String -> if (addVal.isNotBlank()) {
                    Log.d(TAG, "handleFilters: add $addVal")
                    CommunicationFilter.add(addVal, context)
                }
                is org.json.JSONArray -> {
                    for (i in 0 until addVal.length()) {
                        val f = addVal.optString(i)
                        if (f.isNotBlank()) {
                            Log.d(TAG, "handleFilters: add $f")
                            CommunicationFilter.add(f, context)
                        }
                    }
                }
            }
        }

        obj.opt("Remove")?.let { removeVal ->
            when (removeVal) {
                is String -> if (removeVal.isNotBlank()) {
                    Log.d(TAG, "handleFilters: remove $removeVal")
                    CommunicationFilter.remove(removeVal, context)
                }
                is org.json.JSONArray -> {
                    for (i in 0 until removeVal.length()) {
                        val f = removeVal.optString(i)
                        if (f.isNotBlank()) {
                            Log.d(TAG, "handleFilters: remove $f")
                            CommunicationFilter.remove(f, context)
                        }
                    }
                }
            }
        }
    }

    private fun handleComm(obj: JSONObject): Boolean {
        var cleared = false
        if (obj.optBoolean("Clear", false)) {
            Log.d(TAG, "handleComm: clear log")
            CommunicationLog.clear()
            cleared = true
        }

        obj.optJSONObject("Logs")?.let { handleLogSettings(it) }

        if (obj.has("Save")) {
            handleSaveRequest(obj.opt("Save"))
        }
        if (obj.has("Mute")) {
            val mute = obj.optBoolean("Mute")
            val silenced = ScenarioManager.silenced.value
            Log.d(TAG, "handleComm: mute=$mute silenced=$silenced")
            if (mute && !silenced) ScenarioManager.toggleSilence()
            if (!mute && silenced) ScenarioManager.toggleSilence()
        }
        val nfcToggleRaw = when {
            obj.has("NfcEnabled") -> obj.opt("NfcEnabled")
            obj.has("EnableNfc") -> obj.opt("EnableNfc")
            else -> null
        }
        parseFlexibleBoolean(nfcToggleRaw)?.let { enabled ->
            Log.d(TAG, "handleComm: nfcEnabled=$enabled")
            AidManager.setEnabled(enabled)
        }
        when (obj.optString("CurrentScenario")) {
            "Start" -> {
                Log.d(TAG, "handleComm: start scenario")
                ScenarioManager.setRunning(true)
            }
            "Stop" -> {
                Log.d(TAG, "handleComm: stop scenario")
                ScenarioManager.setRunning(false)
            }
            "Clear" -> {
                Log.d(TAG, "handleComm: clear current scenario")
                if (ScenarioManager.running.value) {
                    ScenarioManager.setRunning(false)
                }
                ScenarioManager.setCurrent(AppContextHolder.context, null)
            }
        }
        return cleared
    }

    private fun parseFlexibleBoolean(value: Any?): Boolean? {
        return when (value) {
            null, JSONObject.NULL -> null
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> null
        }
    }

    private fun handleLogSettings(obj: JSONObject) {
        val path = obj.optString("Path", obj.optString("path"))
        if (path.isNotBlank()) {
            updateLogPath(path)
        }
        extractMaxStorageMb(obj)?.let { updateMaxStorage(it) }
        if (obj.has("Save") || obj.has("save")) {
            handleSaveRequest(obj.opt("Save") ?: obj.opt("save"))
        }
    }

    private fun handleSaveRequest(request: Any?) {
        when (request) {
            null, JSONObject.NULL -> return
            is Boolean -> if (request) performLogSave()
            is String -> {
                updateLogPath(request)
                performLogSave()
            }
            is JSONObject -> handleSaveObject(request)
        }
    }

    private fun handleSaveObject(obj: JSONObject) {
        val path = obj.optString("Path", obj.optString("path"))
        val hasPath = path.isNotBlank()
        if (hasPath) {
            updateLogPath(path)
        }
        extractMaxStorageMb(obj)?.let { updateMaxStorage(it) }
        val nestedSave = obj.opt("Save") ?: obj.opt("save")
        val shouldSave = when {
            obj.has("Enabled") -> obj.optBoolean("Enabled")
            nestedSave is Boolean -> nestedSave
            hasPath -> true
            else -> false
        }
        if (shouldSave) {
            performLogSave()
        }
    }

    private fun updateLogPath(rawPath: String) {
        val validation = CommunicationLog.validatePath(rawPath)
        if (!validation.isValid) {
            val reason = validation.errorMessage ?: "Invalid path"
            CommunicationLog.add("STATE-COMM: Invalid log path ($rawPath): $reason", true, false)
            Log.d(TAG, "handleComm: invalid log path input=$rawPath reason=$reason")
            return
        }
        val previous = CommunicationLog.logPath.value
        val sanitized = CommunicationLog.setLogPath(validation.sanitized)
        if (sanitized != previous) {
            val directory = CommunicationLog.getResolvedLogDirectoryPath()
            CommunicationLog.add("STATE-COMM: Log directory $directory", true, true)
            Log.d(TAG, "handleComm: log path -> $directory (input=$rawPath)")
        }
    }

    private fun updateMaxStorage(value: Int) {
        val previous = CommunicationLog.maxStorageMb.value
        val applied = CommunicationLog.setMaxStorageMb(value)
        if (applied != previous) {
            CommunicationLog.add("STATE-COMM: Log storage limit ${applied}MB", true, true)
            Log.d(TAG, "handleComm: log maxStorage -> ${applied}MB")
        }
    }

    private fun extractMaxStorageMb(obj: JSONObject): Int? {
        val key = when {
            obj.has("MaxStorageMb") -> "MaxStorageMb"
            obj.has("maxStorageMb") -> "maxStorageMb"
            else -> return null
        }
        val raw = obj.opt(key)
        val value = when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
        return value?.coerceIn(0, 100)
    }

    private fun performLogSave() {
        try {
            val scenario = ScenarioManager.current.value
            val entries = CommunicationLog.entries.value
            val file = CommunicationLog.saveToConfiguredLocation(scenario, entries)
            CommunicationLog.add("STATE-COMM: Log saved ${file.absolutePath}", true, true)
            Log.d(TAG, "handleComm: saved ${file.absolutePath}")
        } catch (e: Exception) {
            Log.d(TAG, "handleComm save error: ${e.message}")
            CommunicationLog.add("STATE-COMM: Save error (${e.message})", true, false)
        }
    }

    private fun handleScenarios(obj: JSONObject) {
        val context = AppContextHolder.context
        if (obj.optBoolean("Clear", false)) {
            Log.d(TAG, "handleScenarios: clear all")
            ScenarioManager.clearScenarios(context)
        }
        obj.opt("Add")?.let { addVal ->
            when (addVal) {
                is JSONObject -> parseScenario(addVal)?.let {
                    Log.d(TAG, "handleScenarios: add ${it.name}")
                    ScenarioManager.addScenario(context, it)
                }
                is org.json.JSONArray -> {
                    for (i in 0 until addVal.length()) {
                        val scenObj = addVal.optJSONObject(i) ?: continue
                        parseScenario(scenObj)?.let {
                            Log.d(TAG, "handleScenarios: add ${it.name}")
                            ScenarioManager.addScenario(context, it)
                        }
                    }
                }
            }
        }

        obj.opt("Remove")?.let { removeVal ->
            when (removeVal) {
                is String -> if (removeVal.isNotBlank()) {
                    Log.d(TAG, "handleScenarios: remove $removeVal")
                    ScenarioManager.removeScenario(context, removeVal)
                }
                is org.json.JSONArray -> {
                    for (i in 0 until removeVal.length()) {
                        val name = removeVal.optString(i)
                        if (name.isNotBlank()) {
                            Log.d(TAG, "handleScenarios: remove $name")
                            ScenarioManager.removeScenario(context, name)
                        }
                    }
                }
            }
        }
        val current = obj.optString("Current")
        if (current.isNotBlank()) {
            Log.d(TAG, "handleScenarios: setCurrent $current")
            ScenarioManager.setCurrent(context, current)
        }
    }

    private fun handleReset() {
        val context = AppContextHolder.context
        ScenarioManager.setRunning(false)
        ScenarioManager.clearScenarios(context)
        try {
            AidManager.clear()
        } catch (_: UninitializedPropertyAccessException) {
            Log.d(TAG, "handleReset: AidManager not initialized")
        }
        CommunicationFilter.clear(context)
        CommunicationLog.clear()
        CommunicationLog.add("STATE-APP: Reset executed.", true, true)
    }

    private fun parseScenario(obj: JSONObject): Scenario? {
        val name = obj.optString("name")
        if (name.isBlank()) {
            Log.d(TAG, "parseScenario: missing name")
            return null
        }
        val aid = obj.optString("aid")
        val selectOnce = obj.optBoolean("selectOnce", false)
        val steps = mutableListOf<Step>()
        val arr = obj.optJSONArray("steps")
        if (arr != null) {
            val map = mutableMapOf<String, Step>()
            for (i in 0 until arr.length()) {
                val stepObj = arr.optJSONObject(i) ?: continue
                val stepName = stepObj.optString("name")
                if (stepName.isBlank() || map.containsKey(stepName)) continue
                map[stepName] = Step(
                    stepName,
                    stepObj.optString("request"),
                    stepObj.optString("response")
                )
            }
            steps.addAll(map.values)
        }
        Log.d(TAG, "parseScenario: $name steps=${steps.size} aid=$aid selectOnce=$selectOnce")
        return Scenario(name, aid, selectOnce, steps.toMutableList().toMutableStateList())
    }
}
