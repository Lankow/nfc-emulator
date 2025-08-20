package com.lnkv.nfcemulator

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScenarioManager {
    private const val PREFS = "scenario_prefs"
    private const val CURRENT_KEY = "currentScenario"

    private val _current = MutableStateFlow<String?>(null)
    val current = _current.asStateFlow()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _current.value = prefs.getString(CURRENT_KEY, null)
    }

    fun setCurrent(context: Context, name: String?) {
        _current.value = name
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(CURRENT_KEY, name).apply()
    }
}
