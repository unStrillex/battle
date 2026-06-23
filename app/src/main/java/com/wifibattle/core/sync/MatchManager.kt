package com.wifibattle.core.sync

import android.util.Log
import com.wifibattle.core.network.NetworkTransport
import com.wifibattle.core.protocol.NetworkMessage
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * MatchManager 默认实现
 *
 * - Host：定时发送心跳、收集玩家 ping、广播状态
 * - Client：接收心跳、回应 pong、发送本地状态/指令
 */
class DefaultMatchManager(
    private val transport: NetworkTransport,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : MatchManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val isMatchRunning = MutableStateFlow(false)
    private var syncJob: Job? = null
    private var heartbeatJob: Job? = null

    private val pingMap = ConcurrentHashMap<String, Long>()
    private val pendingRpc = ConcurrentHashMap<String, kotlinx.coroutines.CompletableDeferred<JsonElement?>>()
    private val rpcHandlers = ConcurrentHashMap<String, suspend (JsonObject) -> JsonElement>()

    init {
        scope.launch {
            transport.messages.collect { msg -> handleMessage(msg) }
        }
    }

    override fun startMatch() {
        if (isMatchRunning.value) return
        isMatchRunning.value = true
        startSync(50L)
        startHeartbeat()
        Log.i(TAG, "Match started")
    }

    override fun endMatch(reason: String) {
        isMatchRunning.value = false
        syncJob?.cancel()
        heartbeatJob?.cancel()
        transport.broadcast(
            NetworkMessage(
                type = NetworkMessage.MessageType.GAME_END,
                from = "host",
                payload = buildJsonObject { put("reason", reason) }
            )
        )
        Log.i(TAG, "Match ended: $reason")
    }

    override fun broadcastState(state: JsonObject) {
        transport.broadcast(
            NetworkMessage(
                type = NetworkMessage.MessageType.GAME_STATE,
                from = "host",
                payload = state
            )
        )
    }

    override fun broadcastCommand(player: NetworkPlayer, frame: Long, command: JsonObject) {
        val payload = buildJsonObject {
            put("frame", frame)
            put("playerId", player.id)
            put("command", command)
        }
        transport.broadcast(
            NetworkMessage(
                type = NetworkMessage.MessageType.GAME_COMMAND,
                from = player.id,
                payload = payload
            )
        )
    }

    override suspend fun callRpc(target: String?, method: String, args: JsonObject): JsonElement? {
        val callId = UUID.randomUUID().toString()
        val deferred = kotlinx.coroutines.CompletableDeferred<JsonElement?>()
        pendingRpc[callId] = deferred
        val payload = buildJsonObject {
            put("callId", callId)
            put("method", method)
            put("args", args)
        }
        transport.broadcast(
            NetworkMessage(
                type = NetworkMessage.MessageType.GAME_RPC,
                from = "host",
                to = target,
                payload = payload
            )
        )
        return try {
            kotlinx.coroutines.withTimeout(5000) { deferred.await() }
        } catch (e: Exception) {
            Log.w(TAG, "RPC timeout: $method", e)
            null
        } finally {
            pendingRpc.remove(callId)
        }
    }

    fun registerRpc(method: String, handler: suspend (JsonObject) -> JsonElement) {
        rpcHandlers[method] = handler
    }

    fun getPing(playerId: String): Long = pingMap[playerId] ?: -1L

    private fun startSync(intervalMs: Long) {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive && isMatchRunning.value) {
                // 由具体 GameAdapter 在外部定期调用 broadcastState
                delay(intervalMs)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isMatchRunning.value) {
                transport.broadcast(
                    NetworkMessage(
                        type = NetworkMessage.MessageType.PING,
                        from = "host",
                        payload = buildJsonObject { put("t", System.currentTimeMillis()) }
                    )
                )
                delay(3000)
            }
        }
    }

    private suspend fun handleMessage(msg: NetworkMessage) {
        when (msg.type) {
            NetworkMessage.MessageType.PING -> {
                transport.broadcast(
                    NetworkMessage(
                        type = NetworkMessage.MessageType.PONG,
                        from = "host",
                        to = msg.from,
                        payload = buildJsonObject {
                            put("t", msg.payload["t"] ?: 0L)
                            put("echo", System.currentTimeMillis())
                        }
                    )
                )
            }
            NetworkMessage.MessageType.PONG -> {
                val sent = msg.payload["t"]?.toString()?.toLongOrNull() ?: 0L
                if (sent > 0) {
                    val rtt = System.currentTimeMillis() - sent
                    pingMap[msg.from] = rtt
                }
            }
            NetworkMessage.MessageType.GAME_RPC -> {
                val callId = msg.payload["callId"]?.toString()?.trim('"') ?: return
                val method = msg.payload["method"]?.toString()?.trim('"') ?: return
                val args = msg.payload["args"] as? JsonObject ?: JsonObject(emptyMap())
                val handler = rpcHandlers[method]
                if (handler != null) {
                    val result = handler(args)
                    transport.broadcast(
                        NetworkMessage(
                            type = NetworkMessage.MessageType.GAME_RPC,
                            from = "host",
                            to = msg.from,
                            payload = buildJsonObject {
                                put("callId", callId)
                                put("result", result ?: buildJsonObject {})
                            }
                        )
                    )
                }
            }
            else -> {}
        }
    }

    companion object { private const val TAG = "DefaultMatchManager" }
}
