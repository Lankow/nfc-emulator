package com.lnkv.nfcemulator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests for adding and removing communication filters through the server handler. */
class ServerJsonHandlerFilterTest {
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppContextHolder.init(context)
        CommunicationFilter.clear(context)
    }

    @Test
    fun addFilterViaJson() {
        ServerJsonHandler.handle("{\"Type\":\"Filters\",\"Add\":\"AABB\"}")
        assertTrue(CommunicationFilter.filters.value.contains("AABB"))
    }

    @Test
    fun removeFilterViaJson() {
        ServerJsonHandler.handle("{\"Type\":\"Filters\",\"Add\":\"AABB\"}")
        ServerJsonHandler.handle("{\"Type\":\"Filters\",\"Remove\":\"AABB\"}")
        assertFalse(CommunicationFilter.filters.value.contains("AABB"))
    }
}
