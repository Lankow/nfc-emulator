package com.lnkv.nfcemulator

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests the server-driven reset command. */
class ServerJsonHandlerResetTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        AppContextHolder.init(context)

        CommunicationLog.add("test", true)
        CommunicationFilter.add("AA", context)
        ScenarioManager.addScenario(
            context,
            Scenario("S1", "", false, mutableStateListOf())
        )
        ScenarioManager.setCurrent(context, "S1")
    }

    @Test
    fun resetClearsState() {
        ServerJsonHandler.handle("{\"Type\":\"Reset\"}")

        assertTrue(CommunicationLog.entries.value.isEmpty())
        assertTrue(CommunicationFilter.filters.value.isEmpty())
        ScenarioManager.load(context)
        assertNull(ScenarioManager.current.value)
    }
}

