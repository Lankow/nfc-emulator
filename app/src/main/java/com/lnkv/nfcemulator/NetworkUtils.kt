package com.lnkv.nfcemulator

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun getLocalIpAddress(context: Context): String? {
    return try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wm.connectionInfo?.ipAddress ?: return null
        if (ipInt == 0) null else {
            val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
            InetAddress.getByAddress(bytes).hostAddress
        }
    } catch (_: Exception) {
        null
    }
}
