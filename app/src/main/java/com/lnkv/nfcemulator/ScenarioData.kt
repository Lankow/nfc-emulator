package com.lnkv.nfcemulator

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data models and persistence helpers for scenario management.
 */
data class Step(
    var name: String,
    var request: String = "",
    var response: String = "",
)

data class Scenario(
    var name: String,
    var aid: String,
    var selectOnce: Boolean = false,
    val steps: SnapshotStateList<Step> = mutableStateListOf(),
)

internal const val SCENARIO_PREFS = "scenario_prefs"
internal const val SCENARIO_KEY = "scenarios"

internal fun saveScenarios(context: Context, scenarios: List<Scenario>) {
    val serialized = scenarios.map { scenario ->
        val stepString = scenario.steps.joinToString(",") { step ->
            listOf(
                step.name,
                step.request,
                step.response,
            ).joinToString(";")
        }
        listOf(scenario.name, scenario.aid, scenario.selectOnce.toString()).joinToString(";") + "|" + stepString
    }.toSet()
    val prefs = context.getSharedPreferences(SCENARIO_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(SCENARIO_KEY, serialized).apply()
}

internal fun loadScenarios(context: Context): SnapshotStateList<Scenario> {
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
                    sp.getOrElse(2) { "" },
                )
            }.associateBy { it.name }
                .values
                .toMutableStateList()
        } else mutableStateListOf()
        Scenario(name = name, aid = aid, selectOnce = selectOnce, steps = steps)
    }.toMutableStateList()
}

internal fun exportScenarios(context: Context, scenarios: List<Scenario>, uri: Uri) {
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
                                },
                            )
                        }
                    })
                },
            )
        }
    }.toString()
    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    val name = uri.lastPathSegment ?: "file"
    Toast.makeText(context, "Scenarios saved to file: $name", Toast.LENGTH_SHORT).show()
}

internal fun importScenarios(context: Context, uri: Uri): List<Scenario> {
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
                stepObj.optString("response"),
            )
        }
        val selectOnce = obj.optBoolean("selectOnce", false)
        scenarios.removeAll { it.name == name }
        scenarios.add(Scenario(name, obj.optString("aid"), selectOnce, map.values.toMutableList().toMutableStateList()))
    }
    return scenarios
}

