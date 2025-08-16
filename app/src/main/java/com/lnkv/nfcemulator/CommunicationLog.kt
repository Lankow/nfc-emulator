package com.lnkv.nfcemulator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Holds APDU communication logs between the external reader and the emulator.
 */
object CommunicationLog {
    data class Entry(val message: String, val isRequest: Boolean)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries = _entries.asStateFlow()

    /**
     * Appends a new log entry.
     * @param message Hex representation of APDU data.
     * @param isRequest True if the message came from the external reader.
     */
    fun add(message: String, isRequest: Boolean) {
        _entries.value = _entries.value + Entry(message, isRequest)
    }

    /**
     * Clears all stored log entries. Useful for resetting state in tests or when
     * starting a new emulation session.
     */
    fun clear() {
        _entries.value = emptyList()
    }

    /**
     * Writes all log entries to the provided [file], each message separated by a newline.
     */
    fun saveToFile(file: File) {
        val text = _entries.value.joinToString("\n") { it.message }
        file.writeText(text)
    }
}
