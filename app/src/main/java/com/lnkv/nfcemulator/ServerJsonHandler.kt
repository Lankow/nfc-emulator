package com.lnkv.nfcemulator

import androidx.compose.runtime.toMutableStateList
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ServerJsonHandler {
    fun handle(jsonStr: String) {
        try {
            val obj = JSONObject(jsonStr)
            when (obj.optString("Type")) {
                "Aid" -> handleAid(obj)
                "Comm" -> handleComm(obj)
                "Scenarios" -> handleScenarios(obj)
            }
        } catch (e: Exception) {
            CommunicationLog.add("JSON ERR: ${e.message}", true, false)
        }
    }

    private fun handleAid(obj: JSONObject) {
        val clear = obj.optBoolean("Clear", false)
        if (clear) AidManager.clear()

        obj.opt("Add")?.let { addVal ->
            when (addVal) {
                is String -> if (addVal.isNotBlank()) AidManager.add(addVal)
                is org.json.JSONArray -> {
                    for (i in 0 until addVal.length()) {
                        val aid = addVal.optString(i)
                        if (aid.isNotBlank()) AidManager.add(aid)
                    }
                }
            }
        }

        obj.opt("Remove")?.let { removeVal ->
            when (removeVal) {
                is String -> if (removeVal.isNotBlank()) AidManager.remove(removeVal)
                is org.json.JSONArray -> {
                    for (i in 0 until removeVal.length()) {
                        val aid = removeVal.optString(i)
                        if (aid.isNotBlank()) AidManager.remove(aid)
                    }
                }
            }
        }
    }

    private fun handleComm(obj: JSONObject) {
        if (obj.optBoolean("Clear", false)) {
            CommunicationLog.clear()
        }
        if (obj.optBoolean("Save", false)) {
            try {
                val scenario = ScenarioManager.current.value ?: "NoScenario"
                val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "${scenario}_${formatter.format(Date())}.log"
                val dir = AppContextHolder.context.getExternalFilesDir(null)
                    ?: AppContextHolder.context.filesDir
                val file = File(dir, fileName)
                CommunicationLog.saveToFile(file)
                CommunicationLog.add("STATE-COMM: Log saved ${file.absolutePath}", true, true)
            } catch (e: Exception) {
                CommunicationLog.add("STATE-COMM: Save error (${e.message})", true, false)
            }
        }
        if (obj.has("Mute")) {
            val mute = obj.optBoolean("Mute")
            val silenced = ScenarioManager.silenced.value
            if (mute && !silenced) ScenarioManager.toggleSilence()
            if (!mute && silenced) ScenarioManager.toggleSilence()
        }
        when (obj.optString("CurrentScenario")) {
            "Start" -> ScenarioManager.setRunning(true)
            "Stop" -> ScenarioManager.setRunning(false)
            "Clear" -> ScenarioManager.setCurrent(AppContextHolder.context, null)
        }
    }

    private fun handleScenarios(obj: JSONObject) {
        val context = AppContextHolder.context
        if (obj.optBoolean("Clear", false)) {
            ScenarioManager.clearScenarios(context)
        }
        obj.opt("Add")?.let { addVal ->
            when (addVal) {
                is JSONObject -> parseScenario(addVal)?.let { ScenarioManager.addScenario(context, it) }
                is org.json.JSONArray -> {
                    for (i in 0 until addVal.length()) {
                        val scenObj = addVal.optJSONObject(i) ?: continue
                        parseScenario(scenObj)?.let { ScenarioManager.addScenario(context, it) }
                    }
                }
            }
        }

        obj.opt("Remove")?.let { removeVal ->
            when (removeVal) {
                is String -> if (removeVal.isNotBlank()) ScenarioManager.removeScenario(context, removeVal)
                is org.json.JSONArray -> {
                    for (i in 0 until removeVal.length()) {
                        val name = removeVal.optString(i)
                        if (name.isNotBlank()) ScenarioManager.removeScenario(context, name)
                    }
                }
            }
        }
        val current = obj.optString("Current")
        if (current.isNotBlank()) {
            ScenarioManager.setCurrent(context, current)
        }
    }

    private fun parseScenario(obj: JSONObject): Scenario? {
        val name = obj.optString("name")
        if (name.isBlank()) return null
        val steps = mutableListOf<Step>()
        val arr = obj.optJSONArray("steps")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val stepObj = arr.optJSONObject(i) ?: continue
                val step = Step(
                    stepObj.optString("name"),
                    StepType.valueOf(stepObj.optString("type", StepType.Select.name)),
                    stepObj.optString("aid"),
                    stepObj.optBoolean("singleSelect", false),
                    stepObj.optString("request"),
                    stepObj.optString("response"),
                    stepObj.optBoolean("needsSelection", false)
                )
                steps.add(step)
            }
        }
        return Scenario(name, steps.toMutableList().toMutableStateList())
    }
}
