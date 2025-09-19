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
     * Called once the service is created by Android; logs activation for audit
     * visibility.
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        CommunicationLog.add("STATE-NFC: Activated.", true, true)
    }

    /**
     * Processes a command APDU from the external NFC reader using the
     * active scenario and settings.
     *
     * @param commandApdu Incoming APDU request bytes.
     * @param extras Optional extras supplied by Android.
     * @return Response APDU bytes produced by the current scenario.
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
        val response = ScenarioManager.processApdu(commandApdu)
        if (commandApdu != null) {
            val apduHex = commandApdu.toHex()
            val respHex = response?.toHex()
            Log.d(TAG, "APDU: $apduHex -> $respHex")
            CommunicationLog.add("REQ: $apduHex", false)
            CommunicationLog.add(
                "RESP: ${respHex ?: "null"}",
                false
            )
        }
        return response
    }

    /**
     * Invoked when the NFC link is deactivated; updates logs and scenario state.
     *
     * @param reason Android-defined reason for deactivation.
     */
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
 *
 * @receiver Byte array to format.
 * @return Upper-case hex string.
 */
private fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it.toInt() and 0xFF) }
