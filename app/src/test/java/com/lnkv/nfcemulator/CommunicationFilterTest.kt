package com.lnkv.nfcemulator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit tests for [CommunicationFilter]. */
class CommunicationFilterTest {

    @Before
    fun setup() {
        CommunicationFilter.clear()
    }

    @Test
    fun matchesExactHex() {
        CommunicationFilter.add("AABB")
        assertTrue(CommunicationFilter.shouldHide("REQ: AABB"))
        assertFalse(CommunicationFilter.shouldHide("RESP: AABBAA"))
    }

    @Test
    fun matchesWithWildcard() {
        CommunicationFilter.add("*900")
        assertTrue(CommunicationFilter.shouldHide("11900"))
        assertFalse(CommunicationFilter.shouldHide("90011"))
    }
}

