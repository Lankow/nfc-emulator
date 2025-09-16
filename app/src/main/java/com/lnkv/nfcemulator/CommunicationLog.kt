package com.lnkv.nfcemulator

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.ArrayDeque

/**
 * Holds APDU communication logs between the external reader and the emulator.
 */
object CommunicationLog {
    private const val TAG = "CommunicationLog"
    private const val MAX_ENTRIES = 1000
    private const val PREFS_NAME = "log_settings"
    private const val KEY_PATH = "path"
    private const val KEY_MAX_STORAGE_MB = "max_storage_mb"
    private const val DEFAULT_MAX_STORAGE_MB = 10
    private const val LOGS_DIRECTORY = "logs"

    data class Entry(
        val message: String,
        val isServer: Boolean,
        val isSuccess: Boolean? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val buffer = ArrayDeque<Entry>()
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _logPath = MutableStateFlow("")
    val logPath = _logPath.asStateFlow()

    private val _maxStorageMb = MutableStateFlow(DEFAULT_MAX_STORAGE_MB)
    val maxStorageMb = _maxStorageMb.asStateFlow()

    private lateinit var prefs: SharedPreferences
    private var initialized = false

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

    fun init(context: Context) {
        if (initialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _logPath.value = sanitizePath(prefs.getString(KEY_PATH, "") ?: "")
        _maxStorageMb.value = prefs.getInt(KEY_MAX_STORAGE_MB, DEFAULT_MAX_STORAGE_MB)
            .coerceIn(0, 100)
        initialized = true
    }

    fun setLogPath(rawPath: String, context: Context = AppContextHolder.context): String {
        ensureInitialized(context)
        val sanitized = sanitizePath(rawPath)
        if (_logPath.value != sanitized) {
            _logPath.value = sanitized
            prefs.edit().putString(KEY_PATH, sanitized).apply()
        }
        return sanitized
    }

    fun setMaxStorageMb(value: Int, context: Context = AppContextHolder.context): Int {
        ensureInitialized(context)
        val coerced = value.coerceIn(0, 100)
        if (_maxStorageMb.value != coerced) {
            _maxStorageMb.value = coerced
            prefs.edit().putInt(KEY_MAX_STORAGE_MB, coerced).apply()
        }
        return coerced
    }

    fun getLogRootDirectory(context: Context = AppContextHolder.context): File {
        ensureInitialized(context)
        val base = context.getExternalFilesDir(LOGS_DIRECTORY)
            ?: File(context.filesDir, LOGS_DIRECTORY)
        if (!base.exists()) {
            base.mkdirs()
        }
        return base
    }

    fun getResolvedLogDirectory(context: Context = AppContextHolder.context): File {
        ensureInitialized(context)
        val base = getLogRootDirectory(context)
        val path = _logPath.value
        return if (path.isBlank()) {
            base
        } else {
            File(base, path).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
        }
    }

    fun saveToConfiguredLocation(
        scenario: String?,
        entries: List<Entry> = buffer.toList(),
        context: Context = AppContextHolder.context
    ): File {
        ensureInitialized(context)
        val root = getLogRootDirectory(context)
        enforceStorageLimit(root, _maxStorageMb.value)
        val targetDirectory = getResolvedLogDirectory(context)
        val fileName = buildFileName(scenario)
        val target = File(targetDirectory, fileName)
        saveToFile(target, entries)
        enforceStorageLimit(root, _maxStorageMb.value)
        return target
    }

    /**
     * Writes provided [entries] (or all current entries) to the [file], each message separated by a newline.
     */
    fun saveToFile(file: File, entries: List<Entry> = buffer.toList()) {
        Log.d(TAG, "saveToFile: ${file.path}")
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        file.outputStream().use { stream ->
            saveToStream(stream, entries)
        }
    }

    /**
     * Writes provided [entries] to the supplied [outputStream].
     */
    fun saveToStream(outputStream: OutputStream, entries: List<Entry> = buffer.toList()) {
        val text = entries.joinToString("\n") { it.message }
        outputStream.bufferedWriter().use { writer ->
            writer.write(text)
        }
    }

    fun getResolvedLogDirectoryPath(context: Context = AppContextHolder.context): String {
        return getResolvedLogDirectory(context).absolutePath
    }

    private fun ensureInitialized(context: Context) {
        if (!initialized) {
            init(context)
        }
    }

    private fun sanitizePath(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .split('/', '\\')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .map { segment -> segment.replace(Regex("[^A-Za-z0-9._-]"), "") }
            .filter { it.isNotEmpty() }
            .joinToString(File.separator)
    }

    private fun enforceStorageLimit(root: File, maxStorageMb: Int) {
        if (maxStorageMb <= 0) return
        val maxBytes = maxStorageMb * 1024L * 1024L
        val files = root.walkTopDown()
            .filter { it.isFile && it.extension.equals("log", ignoreCase = true) }
            .sortedBy { it.lastModified() }
            .toMutableList()
        var total = files.sumOf { it.length() }
        var index = 0
        while (total > maxBytes && index < files.size - 1) {
            val file = files[index]
            val size = file.length()
            if (file.delete()) {
                total -= size
            }
            index++
        }
    }

    private fun buildFileName(scenario: String?): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
        val scenarioName = (scenario ?: "log").replace(" ", "_")
        return "${scenarioName}_${formatter.format(Date())}.log"
    }
}
