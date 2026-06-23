package com.wifibattle.core.network

import com.wifibattle.data.model.NetworkPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 心跳 & 断线重连管理器
 *
 * - Host: 定期 ping 所有玩家，超时未响应则标记掉线
 * - Client: 定期 ping 主机，连续失败后尝试重连
 */
class HeartbeatManager(
    private val onPlayerTimeout: (String) -> Unit,
    private val onPingUpdate: (String, Long) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val lastSeen = ConcurrentHashMap<String, Long>()
    private val timeoutMs = 15_000L
    private val checkIntervalMs = 3_000L
    private var job: Job? = null

    private val _networkQuality = MutableStateFlow(NetworkQuality.GOOD)
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                lastSeen.forEach { (id, t) ->
                    if (now - t > timeoutMs) {
                        onPlayerTimeout(id)
                        lastSeen.remove(id)
                    }
                }
                delay(checkIntervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun markAlive(playerId: String) {
        lastSeen[playerId] = System.currentTimeMillis()
    }

    fun recordPing(playerId: String, pingMs: Long) {
        onPingUpdate(playerId, pingMs)
        _networkQuality.update {
            when {
                pingMs < 80 -> NetworkQuality.EXCELLENT
                pingMs < 150 -> NetworkQuality.GOOD
                pingMs < 300 -> NetworkQuality.FAIR
                else -> NetworkQuality.POOR
            }
        }
    }

    fun forget(playerId: String) {
        lastSeen.remove(playerId)
    }

    enum class NetworkQuality { EXCELLENT, GOOD, FAIR, POOR }
}
