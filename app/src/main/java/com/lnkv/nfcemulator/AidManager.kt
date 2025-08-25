package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.SharedPreferences
import android.nfc.cardemulation.CardEmulation

object AidManager {
    private const val PREFS_KEY = "aids"

    lateinit var cardEmulation: CardEmulation
        private set
    lateinit var componentName: ComponentName
        private set
    lateinit var prefs: SharedPreferences
        private set

    fun init(cardEmulation: CardEmulation, componentName: ComponentName, prefs: SharedPreferences) {
        this.cardEmulation = cardEmulation
        this.componentName = componentName
        this.prefs = prefs
    }

    fun registerAids(aids: List<String>) {
        if (aids.isEmpty()) {
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

    fun add(aid: String) {
        val set = prefs.getStringSet(PREFS_KEY, emptySet())!!.toMutableSet()
        set.add(aid)
        prefs.edit().putStringSet(PREFS_KEY, set).apply()
        registerAids(set.toList())
    }

    fun remove(aid: String) {
        val set = prefs.getStringSet(PREFS_KEY, emptySet())!!.toMutableSet()
        set.remove(aid)
        prefs.edit().putStringSet(PREFS_KEY, set).apply()
        registerAids(set.toList())
    }

    fun clear() {
        prefs.edit().putStringSet(PREFS_KEY, emptySet()).apply()
        registerAids(emptyList())
    }
}
