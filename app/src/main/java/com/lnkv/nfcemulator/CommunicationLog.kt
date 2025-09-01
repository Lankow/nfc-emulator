package com.lnkv.nfcemulator

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.collections.ArrayDeque

/**
 * Holds APDU communication logs between the external reader and the emulator.
 */
object CommunicationLog {
    private const val TAG = "CommunicationLog"
    private const val MAX_ENTRIES = 1000

    data class Entry(
        val message: String,
        val isServer: Boolean,
        val isSuccess: Boolean? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val buffer = ArrayDeque<Entry>()
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries = _entries.asStateFlow()

    /**
     * Appends a new log entry.
     * @param message Hex representation of APDU data.
     * @param isServer True if the message came from the server side.
     * @param isSuccess Optional flag indicating success or failure for colored logs.
     */
    fun add(message: String, isServer: Boolean, isSuccess: Boolean? = null) {
        Log.d(TAG, "add: $message")
        buffer.addLast(Entry(message, isServer, isSuccess))
        if (buffer.size > MAX_ENTRIES) {
            buffer.removeFirst()
        }
        _entries.value = buffer.toList()
    }

    /**
     * Clears all stored log entries. Useful for resetting state in tests or when
     * starting a new emulation session.
     */
    fun clear() {
        Log.d(TAG, "clear")
        buffer.clear()
        _entries.value = emptyList()
    }

    /**
     * Writes provided [entries] (or all current entries) to the [file], each message separated by a newline.
     */
    fun saveToFile(file: File, entries: List<Entry> = buffer.toList()) {
        Log.d(TAG, "saveToFile: ${file.path}")
        val text = entries.joinToString("\n") { it.message }
        file.writeText(text)
    }
}
