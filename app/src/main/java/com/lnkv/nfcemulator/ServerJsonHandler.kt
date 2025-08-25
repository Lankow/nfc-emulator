package com.lnkv.nfcemulator

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
                // "Scenarios" handling can be implemented later
            }
        } catch (e: Exception) {
            CommunicationLog.add("JSON ERR: ${e.message}", true, false)
        }
    }

    private fun handleAid(obj: JSONObject) {
        val add = obj.optString("Add")
        val remove = obj.optString("Remove")
        val clear = obj.optBoolean("Clear", false)
        if (clear) AidManager.clear()
        if (add.isNotBlank()) AidManager.add(add)
        if (remove.isNotBlank()) AidManager.remove(remove)
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
}
