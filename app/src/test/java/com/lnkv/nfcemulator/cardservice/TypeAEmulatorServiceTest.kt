package com.lnkv.nfcemulator.cardservice

import com.lnkv.nfcemulator.CommunicationLog
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for [TypeAEmulatorService].
 */
class TypeAEmulatorServiceTest {

    private lateinit var service: TypeAEmulatorService

    @Before
    fun setUp() {
        service = TypeAEmulatorService()
        CommunicationLog.clear()
    }

    @Test
    fun processCommandApdu_select_returnsSuccessAndLogs() {
        val select = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        val response = service.processCommandApdu(select, null)

        assertArrayEquals(byteArrayOf(0x90.toByte(), 0x00.toByte()), response)
        val entries = CommunicationLog.entries.value
        assertEquals("REQ: 00A40400", entries[0].message)
        assertEquals(false, entries[0].isServer)
        assertEquals("RESP: 9000", entries[1].message)
        assertEquals(false, entries[1].isServer)
    }

    @Test
    fun processCommandApdu_unknown_returnsError() {
        val command = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00)
        val response = service.processCommandApdu(command, null)

        assertArrayEquals(byteArrayOf(0x6A.toByte(), 0x82.toByte()), response)
    }
}
