package com.lnkv.nfcemulator

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CommunicationLog].
 */
class CommunicationLogTest {

    @Before
    fun setUp() {
        CommunicationLog.clear()
    }

    @Test
    fun addAppendsEntry() {
        CommunicationLog.add("DATA", true)
        val entries = CommunicationLog.entries.value
        assertEquals(1, entries.size)
        assertEquals(CommunicationLog.Entry("DATA", true), entries[0])
    }

    @Test
    fun clearRemovesAllEntries() {
        CommunicationLog.add("ONE", true)
        CommunicationLog.add("TWO", false)
        CommunicationLog.clear()
        assertEquals(0, CommunicationLog.entries.value.size)
    }
}
