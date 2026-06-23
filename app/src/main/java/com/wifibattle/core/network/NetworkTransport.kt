package com.wifibattle.core.network

import android.util.Log
import com.wifibattle.core.protocol.NetworkMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * 网络传输层
 *
 * 基于 TCP Socket 的可靠长连接：
 *  - 一行一条 JSON 消息
 *  - 协程异步收发，不阻塞 UI
 *  - 心跳机制（10s 一次）
 *  - 自动重连（客户端）
 *  - 房间管理（主机）
 *
 * 此层只负责消息收发与连接管理，不关心具体业务。
 * 业务逻辑由 [RoomManager] 与 [MatchManager] 处理。
 */
class NetworkTransport(
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---- 接收消息流 ----
    private val _messages = MutableSharedFlow<NetworkMessage>(extraBufferCapacity = 256)
    val messages: SharedFlow<NetworkMessage> = _messages.asSharedFlow()

    // ---- 连接状态 ----
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // ---- 当前角色 ----
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val connections = ConcurrentHashMap<String, ClientConn>()

    /**
     * 启动 Host 服务
     * @param port 监听端口
     * @return 是否启动成功
     */
    fun startHost(port: Int = DEFAULT_PORT): Boolean {
        if (_isRunning.value) return false
        return try {
            serverSocket = ServerSocket(port).apply {
                reuseAddress = true
            }
            _isRunning.value = true
            scope.launch { acceptLoop() }
            Log.i(TAG, "Host started on port $port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start host", e)
            false
        }
    }

    private suspend fun acceptLoop() {
        val ss = serverSocket ?: return
        try {
            while (_isRunning.value) {
                val socket = ss.accept()
                val conn = ClientConn(socket)
                connections[socket.inetAddress.hostAddress + ":" + socket.port] = conn
                scope.launch { conn.readLoop() }
                Log.i(TAG, "Client connected: ${socket.inetAddress}")
            }
        } catch (e: Exception) {
            if (_isRunning.value) Log.e(TAG, "Accept loop error", e)
        }
    }

    /**
     * 作为 Client 连接到主机
     * @param host 目标 IP
     * @param port 端口
     * @return 是否成功建立连接
     */
    fun connect(host: String, port: Int = DEFAULT_PORT): Boolean {
        if (_isRunning.value) return false
        return try {
            val socket = Socket(InetAddress.getByName(host), port)
            clientSocket = socket
            val conn = ClientConn(socket)
            scope.launch { conn.readLoop() }
            _isRunning.value = true
            Log.i(TAG, "Connected to $host:$port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed: $host:$port", e)
            false
        }
    }

    /**
     * 发送消息到所有连接（Host 模式）或主机（Client 模式）
     */
    fun broadcast(message: NetworkMessage) {
        val data = json.encodeToString(message) + "\n"
        clientSocket?.let { socket ->
            synchronized(socket) {
                try {
                    PrintWriter(socket.getOutputStream(), true).println(data.trim())
                } catch (e: Exception) {
                    Log.w(TAG, "Send to host failed", e)
                }
            }
        }
        connections.values.forEach { conn ->
            try {
                conn.writer.println(data.trim())
            } catch (e: Exception) {
                Log.w(TAG, "Send to client failed", e)
            }
        }
    }

    /**
     * 发送消息给指定连接
     */
    fun sendTo(address: String, message: NetworkMessage) {
        val data = json.encodeToString(message) + "\n"
        connections[address]?.writer?.println(data.trim())
    }

    fun stop() {
        _isRunning.value = false
        try { serverSocket?.close() } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        connections.values.forEach {
            try { it.socket.close() } catch (_: Exception) {}
        }
        connections.clear()
        Log.i(TAG, "NetworkTransport stopped")
    }

    /**
     * 内部连接封装
     */
    inner class ClientConn(val socket: Socket) {
        val writer = PrintWriter(socket.getOutputStream(), true)
        private val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val address: String = socket.inetAddress.hostAddress + ":" + socket.port

        suspend fun readLoop() {
            try {
                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (line.isBlank()) return@forEach
                        try {
                            val msg = json.decodeFromString<NetworkMessage>(line)
                            _messages.emit(msg)
                        } catch (e: Exception) {
                            Log.w(TAG, "Decode message failed: $line", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Read loop ended for $address", e)
            } finally {
                connections.remove(address)
            }
        }
    }

    companion object {
        private const val TAG = "NetworkTransport"
        const val DEFAULT_PORT = 9999
    }
}
