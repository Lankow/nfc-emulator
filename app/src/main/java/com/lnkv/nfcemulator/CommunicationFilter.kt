package com.lnkv.nfcemulator

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores user defined hex patterns that should hide matching communication
 * entries from the log. Patterns may contain hexadecimal characters and `*`
 * wildcards. Filters are persisted between app runs via [android.content.SharedPreferences].
 */
object CommunicationFilter {
    private const val PREFS = "comm_filter_prefs"
    private const val KEY = "filters"

    private val _filters = MutableStateFlow<List<String>>(emptyList())
    val filters = _filters.asStateFlow()

    /** Loads stored filters from [Context] preferences. */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _filters.value = prefs.getStringSet(KEY, emptySet())!!.toList()
    }

    /** Adds a [pattern] to the filter set if it is valid. */
    fun add(pattern: String, context: Context? = null) {
        val cleaned = pattern.uppercase()
        if (!isValid(cleaned)) return
        val newSet = (_filters.value + cleaned).toSet()
        _filters.value = newSet.toList()
        context?.let { save(it, newSet) }
    }

    /** Removes all stored filters. */
    fun clear(context: Context? = null) {
        _filters.value = emptyList()
        context?.let { save(it, emptySet()) }
    }

    /** Returns true if [message] should be hidden based on current filters. */
    fun shouldHide(message: String): Boolean {
        val hex = message.substringAfter(":").replace(" ", "").uppercase()
        return _filters.value.any { patternMatches(it, hex) }
    }

    private fun patternMatches(pattern: String, message: String): Boolean {
        val regex = "^" + pattern.replace("*", "[0-9A-F]*") + "$"
        return Regex(regex).matches(message)
    }

    private fun isValid(pattern: String): Boolean {
        return pattern.matches(Regex("[0-9A-F*]+"))
    }

    private fun save(context: Context, filters: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY, filters).apply()
    }
}

