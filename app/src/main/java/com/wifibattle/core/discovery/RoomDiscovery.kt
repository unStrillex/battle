package com.wifibattle.core.discovery

import android.util.Log
import com.wifibattle.data.model.NetworkRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.DatagramSocket
import java.util.Collections

/**
 * 房间自动发现 - UDP 广播方案
 *
 * Host: 周期性向局域网广播地址 (255.255.255.255) 发送房间信息
 * Client: 监听广播端口，收集所有可用房间
 *
 * 该方案兼容所有 Android 设备，无需 mDNS/NSD 复杂配置。
 */
class RoomDiscovery(
    private val port: Int = 9998,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _discoveredRooms = MutableStateFlow<List<NetworkRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<NetworkRoom>> = _discoveredRooms.asStateFlow()

    private var broadcasterJob: Job? = null
    private var listenerJob: Job? = null
    private var multicastSocket: MulticastSocket? = null

    /**
     * Host：开始广播房间
     */
    fun startBroadcasting(room: NetworkRoom) {
        stopBroadcasting()
        broadcasterJob = scope.launch {
            val socket = DatagramSocket().apply {
                broadcast = true
                reuseAddress = true
            }
            val payload = json.encodeToString(room).toByteArray()
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            while (isActive) {
                try {
                    val packet = DatagramPacket(
                        payload, payload.size, broadcastAddr, port
                    )
                    socket.send(packet)
                } catch (e: Exception) {
                    Log.w(TAG, "Broadcast failed", e)
                }
                delay(2000)
            }
            socket.close()
        }
        Log.i(TAG, "Started broadcasting room ${room.name}")
    }

    fun stopBroadcasting() {
        broadcasterJob?.cancel()
        broadcasterJob = null
    }

    /**
     * Client：开始监听房间广播
     */
    fun startListening() {
        stopListening()
        listenerJob = scope.launch {
            try {
                multicastSocket = MulticastSocket(port).apply {
                    broadcast = true
                    reuseAddress = true
                    soTimeout = 5000
                }
                val buffer = ByteArray(4096)
                val seenRooms = mutableMapOf<String, Pair<NetworkRoom, Long>>()
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        multicastSocket?.receive(packet)
                        val data = String(packet.data, 0, packet.length)
                        val room = json.decodeFromString<NetworkRoom>(data)
                        seenRooms[room.id] = room to System.currentTimeMillis()
                        _discoveredRooms.value = seenRooms.values
                            .filter { System.currentTimeMillis() - it.second < 8000 }
                            .map { it.first }
                    } catch (e: java.net.SocketTimeoutException) {
                        // 周期性刷新过期
                        val now = System.currentTimeMillis()
                        seenRooms.values.removeAll { now - it.second >= 8000 }
                        _discoveredRooms.value = seenRooms.values.map { it.first }
                    } catch (e: Exception) {
                        Log.w(TAG, "Listen error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
            }
        }
        Log.i(TAG, "Started listening for rooms")
    }

    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
        try { multicastSocket?.close() } catch (_: Exception) {}
        multicastSocket = null
        _discoveredRooms.value = emptyList()
    }

    /**
     * 工具方法：获取本机在局域网中的 IP
     */
    fun getLocalIpAddress(): String? {
        try {
            Collections.list(NetworkInterface.getNetworkInterfaces()).forEach { intf ->
                Collections.list(intf.inetAddresses).forEach { addr ->
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun shutdown() {
        stopBroadcasting()
        stopListening()
    }

    companion object {
        private const val TAG = "RoomDiscovery"
    }
}
