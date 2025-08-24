package com.lnkv.nfcemulator.cardservice

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.lnkv.nfcemulator.CommunicationLog
import com.lnkv.nfcemulator.SettingsManager

/**
 * Service that handles ISO 14443-4 (APDU) communication. It responds to incoming
 * commands from an NFC reader while the phone is emulating a Type A card.
 */
class TypeAEmulatorService : HostApduService() {

    private var isSelected = false

    /**
     * Processes a command APDU from the external NFC reader. The default
     * behaviour is configurable through [SettingsManager].
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return SettingsManager.unselectedResponse.value.data

        val apduHex = commandApdu.toHex()
        Log.d(TAG, "APDU: $apduHex")
        CommunicationLog.add("REQ: $apduHex", false)

        val response = when {
            isSelectCommand(commandApdu) -> {
                isSelected = true
                SELECT_OK
            }
            isSelected -> SettingsManager.selectedResponse.value.data
            else -> SettingsManager.unselectedResponse.value.data
        }

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
        isSelected = false
    }

    companion object {
        private const val TAG = "TypeAEmulatorService"

        private val SELECT_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
    }
}

/**
 * Helper extension to convert a byte array to a hexadecimal string for logging.
 */
private fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it.toInt() and 0xFF) }
