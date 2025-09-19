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
import android.util.Log
import kotlin.math.max
import java.net.HttpURLConnection

/**
 * Handles the lifecycle of the external automation server connection including
 * socket setup, polling loop management, and follow-up DELETE calls that clear
 * processed commands.
 */
object ServerConnectionManager {
    /** Coroutine scope used to run asynchronous networking tasks. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Logcat tag to simplify filtering connection logs. */
    private const val TAG = "ServerConnMgr"

    /** Human-readable connection state displayed in the UI. */
    var state by mutableStateOf("Disconnected")
        private set

    /** Flag preventing concurrent connect/disconnect operations. */
    var isProcessing by mutableStateOf(false)
        private set

    /** Active TCP socket; `null` when disconnected. */
    private var socket: Socket? = null

    /** Most recent IP address supplied via [connect]. */
    private var currentIp: String? = null

    /** Most recent port used for the connection. */
    private var currentPort: Int? = null

    /** Job running the periodic polling loop. */
    private var pollJob: Job? = null

    /** Body of the last non-empty response received from the server. */
    private var lastResp: String? = null

    /** Version snapshot corresponding to [lastResp]; used for replay logic. */
    private var lastVersion: Long = RequestStateTracker.version

    /**
     * Posts status updates to the connected external server when available.
     *
     * @param status Human-readable status code (e.g. READY, RUNNING).
     */
    fun postStatus(status: String) {
        val ip = currentIp
        val port = currentPort
        if (state == "Connected" && ip != null && port != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        (URL("http://$ip:$port/STATUS").openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json")
                            outputStream.use { it.write("{\"status\":\"$status\"}".toByteArray()) }
                            inputStream.close()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "postStatus error: ${e.message}")
                }
            }
        }
    }

    /**
     * Initiates a socket connection to the remote automation server and starts
     * polling for commands at the given interval.
     *
     * @param context Source context used to resolve connectivity state.
     * @param ip IPv4 address or hostname of the server.
     * @param port TCP port on which the server listens.
     * @param pollingMs Poll interval in milliseconds (minimum 1s enforced).
     */
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
                    Log.d(TAG, "connect: Wi-Fi not connected")
                } else {
                    try {
                        Log.d(TAG, "connect: opening socket to $ip:$port")
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
                        Log.d(TAG, "connect: success")
                        if (pollingMs > 0) {
                            pollJob?.cancel()
                            lastResp = null
                            lastVersion = RequestStateTracker.version
                            // Launch the long-running polling loop that requests new commands.
                            pollJob = scope.launch {
                                while (true) {
                                    try {
                                        val resp = withContext(Dispatchers.IO) {
                                            URL("http://$ip:$port").readText()
                                        }
                                        val version = RequestStateTracker.version
                                        if (resp.isNotBlank()) {
                                            CommunicationLog.add("GET RESP: $resp", true, true)
                                            ServerJsonHandler.handle(resp)
                                            lastResp = resp
                                            lastVersion = RequestStateTracker.version
                                            Log.d(TAG, "poll: $resp")
                                            clearServer(ip, port)
                                        } else if (lastResp != null && version != lastVersion) {
                                            CommunicationLog.add("GET RESP: ${lastResp!!} (replay)", true, true)
                                            ServerJsonHandler.handle(lastResp!!)
                                            lastVersion = RequestStateTracker.version
                                            Log.d(TAG, "poll replay: ${lastResp!!}")
                                        }
                                    } catch (e: Exception) {
                                        CommunicationLog.add(
                                            "STATE-EXT: GET Error (${e.message}).",
                                            true,
                                            false
                                        )
                                        Log.d(TAG, "poll error: ${e.message}")
                                    }
                                    // Wait before the next poll, enforcing a minimum 1-second cadence.
                                    delay(max(pollingMs, 1000L))
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
                        Log.d(TAG, "connect IO error: ${e.message}")
                    } catch (e: Exception) {
                        state = "Encountered Error (${e.message})"
                        CommunicationLog.add(
                            "STATE-EXT: Server Connection $ip:$port Error (${e.message}).",
                            true,
                            false
                        )
                        Log.d(TAG, "connect error: ${e.message}")
                    }
                }
            } catch (e: SecurityException) {
                state = "Encountered Error (${e.message})"
                CommunicationLog.add(
                    "STATE-EXT: Server Connection $ip:$port Error (${e.message}).",
                    true,
                    false
                )
                Log.d(TAG, "connect security error: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Tears down any existing connection to the external server and cancels the
     * polling job so the app can return to a disconnected state.
     */
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
                lastVersion = RequestStateTracker.version
                state = "Disconnected"
                isProcessing = false
                if (ip != null && port != null) {
                    CommunicationLog.add(
                        "STATE-EXT: Server Connection $ip:$port Disconnected.",
                        true,
                        false
                    )
                    Log.d(TAG, "disconnect: $ip:$port")
                } else {
                    CommunicationLog.add(
                        "STATE-EXT: Server Connection Disconnected.",
                        true,
                        false
                    )
                    Log.d(TAG, "disconnect")
                }
            }
        }
    }

    /**
     * Issues an HTTP DELETE to the server so processed commands are cleared from
     * its queue. The call is best-effort and errors are logged for operators.
     *
     * @param ip Target server address to clear.
     * @param port Target server port to clear.
     */
    private fun clearServer(ip: String, port: Int) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    (URL("http://$ip:$port").openConnection() as HttpURLConnection).apply {
                        requestMethod = "DELETE"
                        connectTimeout = 5000
                        readTimeout = 5000
                        inputStream.close()
                    }
                }
                Log.d(TAG, "clearServer: success")
            } catch (e: Exception) {
                CommunicationLog.add("STATE-EXT: DELETE Error (${e.message}).", true, false)
                Log.d(TAG, "clearServer error: ${e.message}")
            }
        }
    }
}

