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
import android.util.Log

object InternalServerManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val TAG = "InternalServer"

    var state by mutableStateOf("Stopped")
        private set
    var isProcessing by mutableStateOf(false)
        private set

    private var serverSocket: ServerSocket? = null
    private val clients = mutableMapOf<String, Socket>()

    fun start(port: Int) {
        if (serverSocket != null || isProcessing) return
        try {
            Log.d(TAG, "start: port=$port")
            val server = ServerSocket(port)
            serverSocket = server
            state = "Running"
            CommunicationLog.add("STATE-INT: Server started on port ${server.localPort}.", true, true)
            scope.launch {
                try {
                    while (!server.isClosed) {
                        val socket = server.accept()
                        val id = "${socket.inetAddress.hostAddress}:${socket.port}"
                        Log.d(TAG, "client connected: $id")
                        clients[id] = socket
                        CommunicationLog.add("STATE-INT: Device $id started transmission.", true, true)
                        launch {
                            var cleared = false
                            try {
                        val reader = socket.getInputStream().bufferedReader()
                        val requestLine = reader.readLine() ?: ""
                        val parts = requestLine.split(" ")
                        val method = parts.getOrElse(0) { "" }
                        val path = parts.getOrElse(1) { "/" }
                        val headers = mutableListOf<String>()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            headers.add(line)
                        }
                        val contentLength = headers.firstOrNull {
                            it.startsWith("Content-Length", ignoreCase = true)
                        }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
                        if (method.equals("GET", true) && path.equals("/STATUS", true)) {
                            val status = AppStatusManager.current
                            val resp = "HTTP/1.1 200 OK\r\nContent-Length: ${status.length}\r\n\r\n$status"
                            try { socket.getOutputStream().apply { write(resp.toByteArray()); flush() } } catch (_: Exception) {}
                        } else if (method.equals("POST", true)) {
                            val bodyChars = CharArray(contentLength)
                            var read = 0
                            while (read < contentLength) {
                                val r = reader.read(bodyChars, read, contentLength - read)
                                if (r == -1) break
                                read += r
                            }
                            val body = String(bodyChars, 0, read)
                            Log.d(TAG, "request: $body")
                            if (body.isNotBlank()) {
                                CommunicationLog.add("POST: $body", true, true)
                                cleared = ServerJsonHandler.handle(body)
                            }
                            try {
                                socket.getOutputStream().apply {
                                    write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nOK".toByteArray())
                                    flush()
                                }
                            } catch (_: Exception) {
                            }
                        }
                            } catch (e: Exception) {
                                Log.d(TAG, "client error: ${e.message}")
                            } finally {
                                clients.remove(id)
                                if (!cleared) {
                                    CommunicationLog.add("STATE-INT: Device $id finished transmission.", true, false)
                                }
                                try { socket.close() } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (e: SocketException) {
                    Log.d(TAG, "server socket closed")
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
            Log.d(TAG, "start error: ${e.message}")
            state = "Error (${e.message})"
            CommunicationLog.add("STATE-INT: Server start error (${e.message}).", true, false)
            AppStatusManager.setError()
        }
    }

    fun stop() {
        if (serverSocket == null || isProcessing) return
        isProcessing = true
        Log.d(TAG, "stop")
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

