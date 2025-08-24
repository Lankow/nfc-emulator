package com.lnkv.nfcemulator.cardservice

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.lnkv.nfcemulator.CommunicationLog
import com.lnkv.nfcemulator.ScenarioManager

/**
 * Service that handles ISO 14443-4 (APDU) communication. It responds to incoming
 * commands from an NFC reader while the phone is emulating a Type A card.
 */
class TypeAEmulatorService : HostApduService() {

    /**
     * Processes a command APDU from the external NFC reader using the
     * active scenario and settings.
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
        val response = ScenarioManager.processApdu(commandApdu)
        if (commandApdu != null) {
            val apduHex = commandApdu.toHex()
            Log.d(TAG, "APDU: $apduHex")
            CommunicationLog.add("REQ: $apduHex", false)
            CommunicationLog.add(
                "RESP: ${response?.toHex() ?: "null"}",
                false
            )
        }
        return response
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
        ScenarioManager.onDeactivated()
    }

    companion object {
        private const val TAG = "TypeAEmulatorService"
    }
}

/**
 * Helper extension to convert a byte array to a hexadecimal string for logging.
 */
private fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it.toInt() and 0xFF) }
