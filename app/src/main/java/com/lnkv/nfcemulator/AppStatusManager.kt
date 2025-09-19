package com.lnkv.nfcemulator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks high level application status exposed via /STATUS endpoint and posted
 * to an external server when connected.
 */
object AppStatusManager {
    /**
     * Mutable flow backing the published status; keeps the last known state so
     * new collectors immediately receive the current value.
     */
    private val _status = MutableStateFlow("IDLE")

    /** Publicly exposed read-only view of [_status] for UI or server clients. */
    val status = _status.asStateFlow()

    /** Current status value. */
    val current: String
        get() = _status.value

    /**
     * Updates the tracked status value and notifies connected listeners.
     *
     * @param value New status string.
     */
    private fun set(value: String) {
        if (_status.value != value) {
            _status.value = value
            // Notify external server if connected
            ServerConnectionManager.postStatus(value)
        }
    }

    /** Marks that a scenario was loaded and the app is ready to run. */
    fun scenarioLoaded() {
        set("READY")
    }

    /** Marks that no scenario is loaded so the app is idle. */
    fun scenarioUnloaded() {
        set("IDLE")
    }

    /** Marks that the active scenario is currently running. */
    fun scenarioRunning() {
        set("RUNNING")
    }

    /** Marks that the previously running scenario completed its flow. */
    fun scenarioFinished() {
        set("FINISHED")
    }

    /** Indicates that a running scenario was stopped manually. */
    fun scenarioStopped() {
        set("STOPPED")
    }

    /**
     * Reports whether the emulator is silenced.
     *
     * @param on `true` when silenced, `false` otherwise.
     */
    fun silenced(on: Boolean) {
        if (on) set("SILENCED") else scenarioUnloadedOrReady()
    }

    /**
     * Helper that infers whether the READY or IDLE state should be reported
     * based on current scenario selection.
     */
    private fun scenarioUnloadedOrReady() {
        if (ScenarioManager.current.value != null) set("READY") else set("IDLE")
    }

    /** Marks that an unrecoverable error occurred. */
    fun setError() {
        set("ERROR")
    }
}

