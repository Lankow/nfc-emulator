package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.SharedPreferences
import android.nfc.cardemulation.CardEmulation
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Maintains the set of registered Application Identifiers (AIDs) and syncs
 * them with Android's [CardEmulation] subsystem.
 *
 * The object centralizes all AID persistence and registration logic so that
 * other components only need to call high level helpers such as [add] or
 * [replaceAll].
 */
object AidManager {
    /**
     * SharedPreferences key used to store the serialized set of AIDs that
     * should be registered.
     */
    private const val PREFS_KEY = "aids"

    /** SharedPreferences key used to persist whether emulation is enabled. */
    private const val ENABLED_KEY = "enabled"

    /** Static tag used for Logcat visibility. */
    private const val TAG = "AidManager"

    /**
     * Handle to the Android system's card emulation interface. It is injected
     * via [init] and reused for every register/remove call.
     */
    lateinit var cardEmulation: CardEmulation
        private set

    /**
     * Component reference to the [TypeAEmulatorService]; required when
     * registering AIDs with the framework.
     */
    lateinit var componentName: ComponentName
        private set

    /** Backing preferences store that persists both AIDs and enablement. */
    lateinit var prefs: SharedPreferences
        private set

    /** Cached enablement flag so quick checks avoid hitting SharedPreferences. */
    private var enabled = true

    /**
     * Internal flow backing [enabledFlow]; allows observers to react to toggle
     * changes.
     */
    private val _enabledState = MutableStateFlow(true)

    /** Public read-only flow describing whether the emulator is enabled. */
    val enabledFlow = _enabledState.asStateFlow()

    /** Indicates whether NFC emulation is currently enabled. */
    val isEnabled: Boolean
        get() = enabled

    /**
     * Initializes the manager with the core NFC APIs and stored preferences.
     *
     * @param cardEmulation [CardEmulation] instance used to register AIDs.
     * @param componentName Component of [TypeAEmulatorService].
     * @param prefs Shared preferences backing AID persistence.
     */
    fun init(cardEmulation: CardEmulation, componentName: ComponentName, prefs: SharedPreferences) {
        Log.d(TAG, "init: component=$componentName")
        this.cardEmulation = cardEmulation
        this.componentName = componentName
        this.prefs = prefs
        enabled = prefs.getBoolean(ENABLED_KEY, true)
        _enabledState.value = enabled
    }

    /**
     * Registers the provided [aids] with Android's card emulation service.
     * Automatically clears registrations when emulation is disabled.
     *
     * @param aids List of AIDs to register.
     */
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

    /**
     * Enables or disables NFC emulation and persists the choice.
     *
     * @param value `true` to enable, `false` to disable.
     */
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

    /**
     * Replaces the registered AIDs with the provided collection.
     *
     * @param aids New collection of AIDs to register and persist.
     */
    fun replaceAll(aids: Collection<String>) {
        val set = aids.toSet()
        prefs.edit().putStringSet(PREFS_KEY, set).apply()
        registerAids(set.toList())
        RequestStateTracker.markChanged()
    }

    /**
     * Adds a single [aid] to the stored collection and updates registrations.
     *
     * @param aid AID value to add.
     */
    fun add(aid: String) {
        Log.d(TAG, "add: $aid")
        val set = prefs.getStringSet(PREFS_KEY, emptySet())!!.toMutableSet()
        set.add(aid)
        prefs.edit().putStringSet(PREFS_KEY, set).apply()
        registerAids(set.toList())
        RequestStateTracker.markChanged()
    }

    /**
     * Removes an [aid] from the stored collection and updates registrations.
     *
     * @param aid AID value to remove.
     */
    fun remove(aid: String) {
        Log.d(TAG, "remove: $aid")
        val set = prefs.getStringSet(PREFS_KEY, emptySet())!!.toMutableSet()
        set.remove(aid)
        prefs.edit().putStringSet(PREFS_KEY, set).apply()
        registerAids(set.toList())
        RequestStateTracker.markChanged()
    }

    /**
     * Clears all stored AIDs and removes them from the card emulation service.
     */
    fun clear() {
        Log.d(TAG, "clear")
        prefs.edit().putStringSet(PREFS_KEY, emptySet()).apply()
        registerAids(emptyList())
        RequestStateTracker.markChanged()
    }
}
