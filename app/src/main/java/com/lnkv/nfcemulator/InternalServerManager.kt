package com.lnkv.nfcemulator

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
                        CommunicationLog.add("STATE-INT: Device $id started transmission.", true, true)
                        launch {
                            try {
                                val reader = socket.getInputStream().bufferedReader()
                                val headers = mutableListOf<String>()
                                while (true) {
                                    val line = reader.readLine() ?: break
                                    if (line.isEmpty()) break
                                    headers.add(line)
                                }
                                val contentLength = headers.firstOrNull {
                                    it.startsWith("Content-Length", ignoreCase = true)
                                }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                                val bodyChars = CharArray(contentLength)
                                var read = 0
                                while (read < contentLength) {
                                    val r = reader.read(bodyChars, read, contentLength - read)
                                    if (r == -1) break
                                    read += r
                                }
                                val body = String(bodyChars, 0, read)
                                if (body.isNotBlank()) {
                                    CommunicationLog.add("POST: $body", true, true)
                                    ServerJsonHandler.handle(body)
                                }
                                try {
                                    socket.getOutputStream().apply {
                                        write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nOK".toByteArray())
                                        flush()
                                    }
                                } catch (_: Exception) {
                                }
                            } catch (_: Exception) {
                            } finally {
                                clients.remove(id)
                                CommunicationLog.add("STATE-INT: Device $id finished transmission.", true, false)
                                try { socket.close() } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (_: SocketException) {
                } finally {
                    try { server.close() } catch (_: Exception) {}
                    clients.values.forEach { try { it.close() } catch (_: Exception) {} }
                    clients.clear()
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
            if (state != "Stopped") {
                state = "Stopped"
                CommunicationLog.add("STATE-INT: Server stopped.", true, false)
            }
            isProcessing = false
        }
    }
}

