package com.lnkv.nfcemulator

import android.content.Context

/**
 * Holds a lazily-initialized application [Context] so that non-Android components
 * (for example managers or singletons) can access resources without needing a
 * reference passed through every call site.
 */
object AppContextHolder {
    /**
     * Application-level [Context] that should outlive all UI components; it is
     * initialized once from [init] and reused by helpers that need filesystem
     * or resource access.
     */
    lateinit var context: Context
        private set

    /**
     * Captures the application [context] for later reuse across the process.
     * The method should be called once during app start (e.g. from
     * [android.app.Application.onCreate]).
     *
     * @param context Application context used throughout the app lifecycle.
     */
    fun init(context: Context) {
        this.context = context
    }
}
