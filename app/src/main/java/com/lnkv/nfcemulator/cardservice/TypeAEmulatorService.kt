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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        CommunicationLog.add("STATE-NFC: Activated.", true, true)
    }

    /**
     * Processes a command APDU from the external NFC reader using the
     * active scenario and settings.
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
        val response = ScenarioManager.processApdu(commandApdu)
        if (commandApdu != null) {
            val apduHex = commandApdu.toHex()
            val respHex = response?.toHex()
            Log.d(TAG, "APDU: $apduHex -> $respHex")
            if (isSelectCommand(commandApdu)) {
                val aid = extractAid(commandApdu)
                CommunicationLog.add("SELECT AID: $aid", false)
            }
            CommunicationLog.add("REQ: $apduHex", false)
            CommunicationLog.add(
                "RESP: ${respHex ?: "null"}",
                false
            )
        }
        return response
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
        CommunicationLog.add("STATE-NFC: Deactivated ($reason).", true, false)
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

private fun isSelectCommand(apdu: ByteArray): Boolean {
    return apdu.size >= 4 &&
        apdu[0] == 0x00.toByte() &&
        apdu[1] == 0xA4.toByte() &&
        apdu[2] == 0x04.toByte()
}

private fun extractAid(apdu: ByteArray): String {
    if (apdu.size < 5) return ""
    val lc = apdu[4].toInt() and 0xFF
    if (apdu.size < 5 + lc) return ""
    return apdu.copyOfRange(5, 5 + lc).toHex()
}
