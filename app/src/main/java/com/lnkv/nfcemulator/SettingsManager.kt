package com.lnkv.nfcemulator

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores user-configurable settings that affect how the emulator responds
 * when an APDU outside of the defined scenario is received.
 */
object SettingsManager {
    private const val PREFS = "settings_prefs"
    private const val KEY_SELECTED = "selected_resp"
    private const val KEY_UNSELECTED = "unselected_resp"

    private val _selectedResponse = MutableStateFlow(DefaultResponse.Success)
    val selectedResponse = _selectedResponse.asStateFlow()

    private val _unselectedResponse = MutableStateFlow(DefaultResponse.FileNotFound)
    val unselectedResponse = _unselectedResponse.asStateFlow()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _selectedResponse.value = DefaultResponse.valueOf(prefs.getString(KEY_SELECTED, DefaultResponse.Success.name)!!)
        _unselectedResponse.value = DefaultResponse.valueOf(prefs.getString(KEY_UNSELECTED, DefaultResponse.FileNotFound.name)!!)
    }

    fun setSelectedResponse(context: Context, resp: DefaultResponse) {
        _selectedResponse.value = resp
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED, resp.name).apply()
    }

    fun setUnselectedResponse(context: Context, resp: DefaultResponse) {
        _unselectedResponse.value = resp
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_UNSELECTED, resp.name).apply()
    }
}

/** Responses available for unexpected APDUs. */
enum class DefaultResponse(val label: String, val data: ByteArray) {
    Success("Success (9000)", byteArrayOf(0x90.toByte(), 0x00.toByte())),
    FileNotFound("File Not Found (6A82)", byteArrayOf(0x6A.toByte(), 0x82.toByte())),
    ConditionsNotSatisfied("Conditions Not Satisfied (6985)", byteArrayOf(0x69.toByte(), 0x85.toByte())),
    IncorrectParameters("Incorrect Parameters (6A86)", byteArrayOf(0x6A.toByte(), 0x86.toByte()))
}
