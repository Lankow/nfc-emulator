package com.lnkv.nfcemulator.cardservice

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class TypeAEmulatorService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return UNKNOWN_COMMAND

        val apduHex = commandApdu.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
        Log.d(TAG, "APDU: $apduHex")

        return if (commandApdu.contentEquals(SELECT_APDU)) SELECT_OK else UNKNOWN_COMMAND
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: ${'$'}reason")
    }

    companion object {
        private const val TAG = "TypeAEmulatorService"

        // ISO 7816-4 SELECT command for our AID (F0010203040506)
        private val SELECT_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
            0x07.toByte(),
            0xF0.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(),
            0x00.toByte()
        )

        private val SELECT_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val UNKNOWN_COMMAND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    }
}