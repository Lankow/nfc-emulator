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
    /** Preference file name used for filter persistence. */
    private const val PREFS = "comm_filter_prefs"

    /** Key within [PREFS] storing the string set of filters. */
    private const val KEY = "filters"

    /**
     * Internal state flow containing the active list of filters. New observers
     * immediately receive the current snapshot.
     */
    private val _filters = MutableStateFlow<List<String>>(emptyList())

    /** Read-only view over [_filters] exposed to UI layers. */
    val filters = _filters.asStateFlow()

    /**
     * Loads stored filters from [context] preferences.
     *
     * @param context Source context providing access to shared preferences.
     */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _filters.value = prefs.getStringSet(KEY, emptySet())!!.toList()
    }

    /**
     * Adds a [pattern] to the filter set if it passes validation.
     *
     * @param pattern Hex or wildcard pattern to persist.
     * @param context Optional context used to store the updated set.
     */
    fun add(pattern: String, context: Context? = null) {
        val cleaned = pattern.uppercase()
        if (!isValid(cleaned)) return
        val newSet = (_filters.value + cleaned).toSet()
        _filters.value = newSet.toList()
        context?.let { save(it, newSet) }
        RequestStateTracker.markChanged()
    }

    /**
     * Removes a [pattern] from the filter set.
     *
     * @param pattern Existing pattern to remove.
     * @param context Optional context used to store the updated set.
     */
    fun remove(pattern: String, context: Context? = null) {
        val newSet = _filters.value.filterNot { it == pattern }.toSet()
        _filters.value = newSet.toList()
        context?.let { save(it, newSet) }
        RequestStateTracker.markChanged()
    }

    /**
     * Replaces an existing [old] pattern with [new].
     *
     * @param old Pattern to remove.
     * @param new Replacement pattern to insert.
     * @param context Optional context used to store the updated set.
     */
    fun replace(old: String, new: String, context: Context? = null) {
        val cleaned = new.uppercase()
        if (!isValid(cleaned)) return
        val newSet = (_filters.value - old + cleaned).toSet()
        _filters.value = newSet.toList()
        context?.let { save(it, newSet) }
        RequestStateTracker.markChanged()
    }

    /**
     * Sets all filters from [list], replacing any existing values.
     *
     * @param list Complete list of patterns to store.
     * @param context Context used to persist the provided filters.
     */
    fun setAll(list: List<String>, context: Context) {
        val cleaned = list.map { it.uppercase() }.filter { isValid(it) }.toSet()
        _filters.value = cleaned.toList()
        save(context, cleaned)
        RequestStateTracker.markChanged()
    }

    /**
     * Removes all stored filters.
     *
     * @param context Optional context used to persist the cleared state.
     */
    fun clear(context: Context? = null) {
        _filters.value = emptyList()
        context?.let { save(it, emptySet()) }
        RequestStateTracker.markChanged()
    }

    /**
     * Evaluates whether the provided [message] should be hidden based on the
     * currently active filters.
     *
     * @param message Log entry containing a prefix and the hex payload.
     * @return `true` when at least one filter matches the message body.
     */
    fun shouldHide(message: String): Boolean {
        // Strip prefixes like "REQ:" and spaces so matching is performed solely on the payload.
        val hex = message.substringAfter(":").replace(" ", "").uppercase()
        return _filters.value.any { patternMatches(it, hex) }
    }

    /**
     * Checks whether a filter [pattern] matches the [message] payload.
     *
     * @param pattern Stored filter expression.
     * @param message Message body stripped of prefixes.
     * @return `true` when the message satisfies the pattern.
     */
    private fun patternMatches(pattern: String, message: String): Boolean {
        // Translate the wildcard syntax into a regular expression matcher.
        val regex = "^" + pattern.replace("*", "[0-9A-F]*") + "$"
        return Regex(regex).matches(message)
    }

    /**
     * Validates that the filter [pattern] only contains hexadecimal characters
     * or the `*` wildcard.
     *
     * @param pattern Pattern to validate.
     * @return `true` when the pattern is acceptable.
     */
    private fun isValid(pattern: String): Boolean {
        return pattern.matches(Regex("[0-9A-F*]+"))
    }

    /**
     * Persists the provided [filters] to shared preferences using the supplied
     * [context].
     *
     * @param context Context providing access to preferences.
     * @param filters Filters to persist.
     */
    private fun save(context: Context, filters: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY, filters).apply()
    }
}

