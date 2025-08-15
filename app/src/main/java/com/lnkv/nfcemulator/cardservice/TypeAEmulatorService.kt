package com.lnkv.nfcemulator.cardservice

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.lnkv.nfcemulator.CommunicationLog

class TypeAEmulatorService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return UNKNOWN_COMMAND

        val apduHex = commandApdu.toHex()
        Log.d(TAG, "APDU: $apduHex")
        CommunicationLog.add("REQ: $apduHex", true)

        val response = if (isSelectCommand(commandApdu)) SELECT_OK else UNKNOWN_COMMAND
        CommunicationLog.add("RESP: ${response.toHex()}", false)

        return response
    }

    private fun isSelectCommand(apdu: ByteArray): Boolean {
        return apdu.size >= 4 &&
            apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x04.toByte()
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: ${'$'}reason")
    }

    companion object {
        private const val TAG = "TypeAEmulatorService"

        private val SELECT_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val UNKNOWN_COMMAND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    }
}

private fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it.toInt() and 0xFF) }