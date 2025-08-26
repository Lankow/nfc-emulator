package com.lnkv.nfcemulator

import androidx.compose.runtime.toMutableStateList
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

object ServerJsonHandler {
    private const val TAG = "ServerJsonHandler"

    fun handle(jsonStr: String) {
        Log.d(TAG, "handle: $jsonStr")
        try {
            val obj = JSONObject(jsonStr)
            when (obj.optString("Type")) {
                "Aid" -> handleAid(obj)
                "Comm" -> handleComm(obj)
                "Scenarios" -> handleScenarios(obj)
            }
        } catch (e: Exception) {
            Log.d(TAG, "parse error: ${e.message}")
            CommunicationLog.add("JSON ERR: ${e.message}", true, false)
        }
    }

    private fun handleAid(obj: JSONObject) {
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

    private fun handleComm(obj: JSONObject) {
        if (obj.optBoolean("Clear", false)) {
            Log.d(TAG, "handleComm: clear log")
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
                Log.d(TAG, "handleComm: saved ${file.absolutePath}")
            } catch (e: Exception) {
                Log.d(TAG, "handleComm save error: ${e.message}")
                CommunicationLog.add("STATE-COMM: Save error (${e.message})", true, false)
            }
        }
        if (obj.has("Mute")) {
            val mute = obj.optBoolean("Mute")
            val silenced = ScenarioManager.silenced.value
            Log.d(TAG, "handleComm: mute=$mute silenced=$silenced")
            if (mute && !silenced) ScenarioManager.toggleSilence()
            if (!mute && silenced) ScenarioManager.toggleSilence()
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
                ScenarioManager.setCurrent(AppContextHolder.context, null)
            }
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

    private fun parseScenario(obj: JSONObject): Scenario? {
        val name = obj.optString("name")
        if (name.isBlank()) {
            Log.d(TAG, "parseScenario: missing name")
            return null
        }
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
        Log.d(TAG, "parseScenario: $name steps=${steps.size}")
        return Scenario(name, steps.toMutableList().toMutableStateList())
    }
}
