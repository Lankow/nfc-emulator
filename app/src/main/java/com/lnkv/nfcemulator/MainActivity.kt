@file:OptIn(ExperimentalMaterial3Api::class)

package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import com.lnkv.nfcemulator.cardservice.TypeAEmulatorService
import com.lnkv.nfcemulator.ui.theme.NFCEmulatorTheme

/**
 * Main activity hosting the overall application and setting up initial state.
 */
class MainActivity : ComponentActivity() {
    private lateinit var cardEmulation: CardEmulation
    private lateinit var componentName: ComponentName
    private lateinit var prefs: SharedPreferences
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "onCreate")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Disable back button to keep the app in the foreground
            }
        })

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        cardEmulation = CardEmulation.getInstance(nfcAdapter)
        componentName = ComponentName(this, TypeAEmulatorService::class.java)
        prefs = getSharedPreferences("nfc_aids", MODE_PRIVATE)

        AppContextHolder.init(applicationContext)
        AidManager.init(cardEmulation, componentName, prefs)

        val storedAids = prefs.getStringSet("aids", setOf("F0010203040506"))!!.toList()
        Log.d(TAG, "storedAids: $storedAids")
        AidManager.registerAids(storedAids)

        val serverPrefs = getSharedPreferences("server_prefs", MODE_PRIVATE)
        if (serverPrefs.getBoolean("isExternal", true) &&
            serverPrefs.getBoolean("autoConnect", false)
        ) {
            val ipPort = serverPrefs.getString("ip", "0.0.0.0:0000")!!
            if (ipPort != "0.0.0.0:0000") {
                val host = ipPort.substringBefore(":")
                val port = ipPort.substringAfter(":").toIntOrNull()
                val poll = serverPrefs.getString("pollingTime", "0")!!.toLongOrNull() ?: 0
                if (port != null) {
                    Log.d(TAG, "autoConnect to $host:$port poll=$poll")
                    ServerConnectionManager.connect(this, host, port, poll)
                }
            }
        }

        if (serverPrefs.getBoolean("autoStart", false) && getLocalIpAddress(this) != null) {
            val staticPort = serverPrefs.getBoolean("staticPort", false)
            val portStr = serverPrefs.getString("port", "0000")!!
            val portNum = if (staticPort && portStr.isNotBlank()) portStr.toIntOrNull() ?: 0 else 0
            if (ServerConnectionManager.state == "Connected") {
                Log.d(TAG, "disconnect before autoStart")
                ServerConnectionManager.disconnect()
            }
            Log.d(TAG, "autoStart server port=$portNum")
            InternalServerManager.start(portNum)
        }

        ScenarioManager.load(this)
        CommunicationFilter.load(this)
        Log.d(TAG, "initialization complete")

        setContent {
            NFCEmulatorTheme {
                MainScreen()
            }
        }
    }
}
