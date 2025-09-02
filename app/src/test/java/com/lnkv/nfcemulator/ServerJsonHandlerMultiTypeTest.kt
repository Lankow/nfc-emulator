package com.lnkv.nfcemulator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Verifies that multiple command types in one request are processed. */
class ServerJsonHandlerMultiTypeTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        AppContextHolder.init(context)
        CommunicationLog.add("test", true)
        CommunicationFilter.clear(context)
    }

    @Test
    fun handlesMultipleSections() {
        val json = "{\"Comm\":{\"Clear\":true},\"Filters\":{\"Add\":\"AABB\"}}"
        ServerJsonHandler.handle(json)
        assertTrue(CommunicationFilter.filters.value.contains("AABB"))
        assertTrue(CommunicationLog.entries.value.isEmpty())
    }
}
