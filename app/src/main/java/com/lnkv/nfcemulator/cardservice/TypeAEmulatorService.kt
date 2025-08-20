package com.lnkv.nfcemulator.cardservice

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.lnkv.nfcemulator.CommunicationLog

/**
 * Service that handles ISO 14443-4 (APDU) communication. It responds to incoming
 * commands from an NFC reader while the phone is emulating a Type A card.
 */
class TypeAEmulatorService : HostApduService() {

    /**
     * Processes a command APDU from the external NFC reader. For this simple example
     * we only react to a SELECT command and return *90 00* (success). Any other
     * command results in *6A 82* (file not found).
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return UNKNOWN_COMMAND

        val apduHex = commandApdu.toHex()
        Log.d(TAG, "APDU: $apduHex")
        CommunicationLog.add("REQ: $apduHex", false)

        val response = if (isSelectCommand(commandApdu)) SELECT_OK else UNKNOWN_COMMAND
        CommunicationLog.add("RESP: ${response.toHex()}", false)

        return response
    }

    /**
     * Checks whether the incoming APDU is a standard SELECT command.
     */
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

/**
 * Helper extension to convert a byte array to a hexadecimal string for logging.
 */
private fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it.toInt() and 0xFF) }
