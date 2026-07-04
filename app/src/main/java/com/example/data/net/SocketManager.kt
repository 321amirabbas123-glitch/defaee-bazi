package com.example.data.net

import android.util.Log
import com.example.domain.model.NetworkMessage
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.Collections

class SocketManager {
    companion object {
        private const val TAG = "SocketManager"
        const val PORT = 9090

        fun getLocalIpAddress(): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val sAddr = addr.hostAddress ?: continue
                            val isIPv4 = sAddr.indexOf(':') < 0
                            if (isIPv4) {
                                return sAddr
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error getting local IP", ex)
            }
            return "127.0.0.1"
        }
    }

    private val moshi = Moshi.Builder().build()
    private val messageAdapter = moshi.adapter(NetworkMessage::class.java)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var serverSocket: ServerSocket? = null
    private var activeServerSocket: Socket? = null
    private var clientSocket: Socket? = null
    
    private var serverWriter: PrintWriter? = null
    private var clientWriter: PrintWriter? = null
    
    var isServerRunning = false
        private set
    var isClientConnected = false
        private set

    // Server Callbacks
    var onClientConnected: ((String) -> Unit)? = null
    var onServerMessageReceived: ((NetworkMessage) -> Unit)? = null
    var onServerDisconnected: (() -> Unit)? = null

    // Client Callbacks
    var onClientConnectionEstablished: (() -> Unit)? = null
    var onClientMessageReceived: ((NetworkMessage) -> Unit)? = null
    var onClientDisconnected: (() -> Unit)? = null

    // --- HOST / SERVER METHODS ---

    fun startServer() {
        if (isServerRunning) return
        isServerRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT).apply {
                    reuseAddress = true
                }
                Log.d(TAG, "Server started on port $PORT")

                while (isServerRunning) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d(TAG, "Client connected from: ${socket.inetAddress.hostAddress}")
                    
                    // Close any previous client socket on Host
                    activeServerSocket?.close()
                    
                    activeServerSocket = socket
                    serverWriter = PrintWriter(socket.getOutputStream(), true)
                    
                    withContext(Dispatchers.Main) {
                        onClientConnected?.invoke(socket.inetAddress.hostAddress ?: "Unknown Client")
                    }

                    // Start reading client messages
                    launch {
                        handleIncomingConnection(socket, isHost = true)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server exception: ${e.message}")
                stopServer()
            }
        }
    }

    fun stopServer() {
        isServerRunning = false
        try {
            serverWriter?.close()
            activeServerSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        } finally {
            serverWriter = null
            activeServerSocket = null
            serverSocket = null
            scope.launch(Dispatchers.Main) {
                onServerDisconnected?.invoke()
            }
        }
    }

    fun sendToClient(message: NetworkMessage) {
        scope.launch {
            try {
                val json = messageAdapter.toJson(message)
                serverWriter?.println(json)
                Log.d(TAG, "Host sent message: ${message.type}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to client", e)
            }
        }
    }

    // --- CLIENT METHODS ---

    fun connectToHost(ipAddress: String) {
        if (isClientConnected) return
        scope.launch {
            try {
                Log.d(TAG, "Connecting to Host at $ipAddress:$PORT...")
                val socket = Socket(ipAddress, PORT)
                clientSocket = socket
                clientWriter = PrintWriter(socket.getOutputStream(), true)
                isClientConnected = true
                
                withContext(Dispatchers.Main) {
                    onClientConnectionEstablished?.invoke()
                }

                // Start reading server messages
                handleIncomingConnection(socket, isHost = false)
            } catch (e: Exception) {
                Log.e(TAG, "Client connection failed", e)
                withContext(Dispatchers.Main) {
                    onClientDisconnected?.invoke()
                }
                disconnectFromHost()
            }
        }
    }

    fun disconnectFromHost() {
        isClientConnected = false
        try {
            clientWriter?.close()
            clientSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting client", e)
        } finally {
            clientWriter = null
            clientSocket = null
            scope.launch(Dispatchers.Main) {
                onClientDisconnected?.invoke()
            }
        }
    }

    fun sendToHost(message: NetworkMessage) {
        scope.launch {
            try {
                val json = messageAdapter.toJson(message)
                clientWriter?.println(json)
                Log.d(TAG, "Client sent message: ${message.type}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to host", e)
            }
        }
    }

    // --- SHARED UTILITY ---

    private suspend fun handleIncomingConnection(socket: Socket, isHost: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (true) {
                    val line = reader.readLine() ?: break // EOF
                    try {
                        val message = messageAdapter.fromJson(line) ?: continue
                        withContext(Dispatchers.Main) {
                            if (isHost) {
                                onServerMessageReceived?.invoke(message)
                            } else {
                                onClientMessageReceived?.invoke(message)
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Failed to parse message: $line", ex)
                    }
                }
            } catch (se: SocketException) {
                Log.d(TAG, "Socket closed or disconnected: ${se.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception reading socket stream: ${e.message}")
            } finally {
                // Connection closed
                withContext(Dispatchers.Main) {
                    if (isHost) {
                        onServerDisconnected?.invoke()
                    } else {
                        onClientDisconnected?.invoke()
                    }
                }
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    fun clear() {
        stopServer()
        disconnectFromHost()
        scope.cancel()
    }
}
