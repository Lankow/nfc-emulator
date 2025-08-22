package com.lnkv.nfcemulator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AidValidationTest {
    @Test
    fun validAidIsAccepted() {
        assertTrue(isValidAid("A0000002471001"))
    }

    @Test
    fun invalidAidIsRejected() {
        assertFalse(isValidAid("AAFFCCDD"))
    }
}
