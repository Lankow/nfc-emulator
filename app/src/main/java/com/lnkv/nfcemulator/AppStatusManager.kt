package com.lnkv.nfcemulator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks high level application status exposed via /STATUS endpoint and posted
 * to an external server when connected.
 */
object AppStatusManager {
    private val _status = MutableStateFlow("IDLE")
    val status = _status.asStateFlow()

    /** Current status value. */
    val current: String
        get() = _status.value

    private fun set(value: String) {
        if (_status.value != value) {
            _status.value = value
            // Notify external server if connected
            ServerConnectionManager.postStatus(value)
        }
    }

    fun scenarioLoaded() {
        set("READY")
    }

    fun scenarioUnloaded() {
        set("IDLE")
    }

    fun scenarioRunning() {
        set("RUNNING")
    }

    fun scenarioFinished() {
        set("FINISHED")
    }

    fun scenarioStopped() {
        set("STOPPED")
    }

    fun silenced(on: Boolean) {
        if (on) set("SILENCED") else scenarioUnloadedOrReady()
    }

    private fun scenarioUnloadedOrReady() {
        if (ScenarioManager.current.value != null) set("READY") else set("IDLE")
    }

    fun setError() {
        set("ERROR")
    }
}

