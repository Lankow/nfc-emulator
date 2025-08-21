package com.lnkv.nfcemulator

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

object InternalServerManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var state by mutableStateOf("Stopped")
        private set
    var isProcessing by mutableStateOf(false)
        private set

    private var serverSocket: ServerSocket? = null
    private val clients = mutableMapOf<String, Socket>()
    val connectedDevices = mutableStateListOf<String>()

    fun start(port: Int) {
        if (serverSocket != null || isProcessing) return
        try {
            val server = ServerSocket(port)
            serverSocket = server
            state = "Running"
            CommunicationLog.add("STATE-INT: Server started on port ${server.localPort}.", true, true)
            scope.launch {
                try {
                    while (!server.isClosed) {
                        val socket = server.accept()
                        val id = "${socket.inetAddress.hostAddress}:${socket.port}"
                        clients[id] = socket
                        connectedDevices.add(id)
                        CommunicationLog.add("STATE-INT: Device connected $id.", true, true)
                        launch {
                            try {
                                socket.getInputStream().readBytes()
                            } catch (_: Exception) {
                            } finally {
                                clients.remove(id)
                                connectedDevices.remove(id)
                                CommunicationLog.add("STATE-INT: Device disconnected $id.", true, false)
                                try { socket.close() } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (_: SocketException) {
                } finally {
                    try { server.close() } catch (_: Exception) {}
                    clients.values.forEach { try { it.close() } catch (_: Exception) {} }
                    clients.clear()
                    connectedDevices.clear()
                    if (state != "Stopped") {
                        state = "Stopped"
                        CommunicationLog.add("STATE-INT: Server stopped.", true, false)
                    }
                }
            }
        } catch (e: Exception) {
            state = "Error (${e.message})"
            CommunicationLog.add("STATE-INT: Server start error (${e.message}).", true, false)
        }
    }

    fun stop() {
        if (serverSocket == null || isProcessing) return
        isProcessing = true
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        } finally {
            serverSocket = null
            clients.values.forEach { try { it.close() } catch (_: Exception) {} }
            clients.clear()
            connectedDevices.clear()
            if (state != "Stopped") {
                state = "Stopped"
                CommunicationLog.add("STATE-INT: Server stopped.", true, false)
            }
            isProcessing = false
        }
    }
}

