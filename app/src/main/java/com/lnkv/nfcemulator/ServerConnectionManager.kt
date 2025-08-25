package com.lnkv.nfcemulator

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

object ServerConnectionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var state by mutableStateOf("Disconnected")
        private set
    var isProcessing by mutableStateOf(false)
        private set

    private var socket: Socket? = null
    private var currentIp: String? = null
    private var currentPort: Int? = null
    private var pollJob: Job? = null
    private var lastResp: String? = null

    fun connect(context: Context, ip: String, port: Int, pollingMs: Long) {
        if (isProcessing) return
        currentIp = ip
        currentPort = port
        state = "Connecting..."
        isProcessing = true
        scope.launch {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val wifiConnected = connectivityManager
                    ?.getNetworkCapabilities(connectivityManager.activeNetwork)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                if (!wifiConnected) {
                    state = "Disconnected"
                    CommunicationLog.add(
                        "STATE-EXT: Server Connection $ip:$port Failed.",
                        true,
                        false
                    )
                } else {
                    try {
                        val s = withContext(Dispatchers.IO) {
                            Socket().apply {
                                connect(InetSocketAddress(ip, port), 5000)
                            }
                        }
                        socket = s
                        state = "Connected"
                        CommunicationLog.add(
                            "STATE-EXT: Server Connection $ip:$port Success.",
                            true,
                            true
                        )
                        if (pollingMs > 0) {
                            pollJob?.cancel()
                            lastResp = null
                            pollJob = scope.launch {
                                while (true) {
                                    try {
                                        val resp = withContext(Dispatchers.IO) {
                                            URL("http://$ip:$port").readText()
                                        }
                                        if (resp.isNotBlank() && resp != lastResp) {
                                            CommunicationLog.add("GET RESP: $resp", true, true)
                                            ServerJsonHandler.handle(resp)
                                            lastResp = resp
                                        }
                                    } catch (e: Exception) {
                                        CommunicationLog.add(
                                            "STATE-EXT: GET Error (${e.message}).",
                                            true,
                                            false
                                        )
                                    }
                                    delay(pollingMs)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        state = "Connection Failed"
                        CommunicationLog.add(
                            "STATE-EXT: Server Connection $ip:$port Failed.",
                            true,
                            false
                        )
                    } catch (e: Exception) {
                        state = "Encountered Error (${e.message})"
                        CommunicationLog.add(
                            "STATE-EXT: Server Connection $ip:$port Error (${e.message}).",
                            true,
                            false
                        )
                    }
                }
            } catch (e: SecurityException) {
                state = "Encountered Error (${e.message})"
                CommunicationLog.add(
                    "STATE-EXT: Server Connection $ip:$port Error (${e.message}).",
                    true,
                    false
                )
            } finally {
                isProcessing = false
            }
        }
    }

    fun disconnect() {
        if (isProcessing) return
        val ip = currentIp
        val port = currentPort
        state = "Connecting..."
        isProcessing = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { socket?.close() }
            } catch (_: Exception) {
            } finally {
                socket = null
                currentIp = null
                currentPort = null
                pollJob?.cancel()
                pollJob = null
                lastResp = null
                state = "Disconnected"
                isProcessing = false
                if (ip != null && port != null) {
                    CommunicationLog.add(
                        "STATE-EXT: Server Connection $ip:$port Disconnected.",
                        true,
                        false
                    )
                } else {
                    CommunicationLog.add(
                        "STATE-EXT: Server Connection Disconnected.",
                        true,
                        false
                    )
                }
            }
        }
    }
}

