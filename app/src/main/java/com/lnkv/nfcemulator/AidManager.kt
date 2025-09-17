package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.SharedPreferences
import android.nfc.cardemulation.CardEmulation
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AidManager {
    private const val PREFS_KEY = "aids"
    private const val ENABLED_KEY = "enabled"
    private const val TAG = "AidManager"

    lateinit var cardEmulation: CardEmulation
        private set
    lateinit var componentName: ComponentName
        private set
    lateinit var prefs: SharedPreferences
        private set

    private var enabled = true
    private val _enabledState = MutableStateFlow(true)
    val enabledFlow = _enabledState.asStateFlow()

    val isEnabled: Boolean
        get() = enabled

    fun init(cardEmulation: CardEmulation, componentName: ComponentName, prefs: SharedPreferences) {
        Log.d(TAG, "init: component=$componentName")
        this.cardEmulation = cardEmulation
        this.componentName = componentName
        this.prefs = prefs
        enabled = prefs.getBoolean(ENABLED_KEY, true)
        _enabledState.value = enabled
    }

    fun registerAids(aids: List<String>) {
        Log.d(TAG, "registerAids: $aids enabled=$enabled")
        if (!enabled) {
            Log.d(TAG, "registerAids: disabled, clearing registered AIDs")
            cardEmulation.removeAidsForService(
                componentName,
                CardEmulation.CATEGORY_OTHER
            )
            return
        }
        if (aids.isEmpty()) {
            Log.d(TAG, "registerAids: clearing all")
            cardEmulation.removeAidsForService(
                componentName,
                CardEmulation.CATEGORY_OTHER
            )
        } else {
            cardEmulation.registerAidsForService(
                componentName,
                CardEmulation.CATEGORY_OTHER,
                aids
            )
        }
    }

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        _enabledState.value = value
        prefs.edit().putBoolean(ENABLED_KEY, value).apply()
        if (value) {
            val aids = prefs.getStringSet(PREFS_KEY, emptySet())!!.toList()
            registerAids(aids)
            CommunicationLog.add("STATE-NFC: Enabled by user.", true, true)
        } else {
            cardEmulation.removeAidsForService(
                componentName,
                CardEmulation.CATEGORY_OTHER
            )
            CommunicationLog.add("STATE-NFC: Disabled by user.", true, false)
        }
        RequestStateTracker.markChanged()
    }

    fun replaceAll(aids: Collection<String>) {
        val set = aids.toSet()
        prefs.edit().putStringSet(PREFS_KEY, set).apply()
        registerAids(set.toList())
        RequestStateTracker.markChanged()
    }

    fun add(aid: String) {
        Log.d(TAG, "add: $aid")
        val set = prefs.getStringSet(PREFS_KEY, emptySet())!!.toMutableSet()
        set.add(aid)
        prefs.edit().putStringSet(PREFS_KEY, set).apply()
        registerAids(set.toList())
        RequestStateTracker.markChanged()
    }

    fun remove(aid: String) {
        Log.d(TAG, "remove: $aid")
        val set = prefs.getStringSet(PREFS_KEY, emptySet())!!.toMutableSet()
        set.remove(aid)
        prefs.edit().putStringSet(PREFS_KEY, set).apply()
        registerAids(set.toList())
        RequestStateTracker.markChanged()
    }

    fun clear() {
        Log.d(TAG, "clear")
        prefs.edit().putStringSet(PREFS_KEY, emptySet()).apply()
        registerAids(emptyList())
        RequestStateTracker.markChanged()
    }
}
