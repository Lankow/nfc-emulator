package com.lnkv.nfcemulator

/**
 * Tracks state changes that should trigger re-processing of identical server
 * responses. Whenever app state affecting server commands is modified, call
 * [markChanged] so that the next poll can reapply the last response if needed.
 */
object RequestStateTracker {
    private var _version: Long = 0

    /** Current state version. Incremented whenever a relevant change occurs. */
    val version: Long
        get() = _version

    /** Mark that app state has changed since the last processed request. */
    fun markChanged() {
        _version++
    }
}

