package com.lnkv.nfcemulator

import android.content.Context

object AppContextHolder {
    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context
    }
}
