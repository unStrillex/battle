package com.wifibattle.adapter.examples

import com.wifibattle.adapter.BaseGameAdapter
import com.wifibattle.data.model.NetworkPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 坦克大战 - 示例 GameAdapter
 *
 * 特性：
 *  - 适合 2~8 人
 *  - 同步频率 20Hz
 *  - 使用 Lockstep 指令同步 + 状态同步双通道
 *  - 展示真实可运行的游戏逻辑
 */
class TankBattleAdapter : BaseGameAdapter() {

    override val gameId = "tank_battle"
    override val displayName = "坦克大战"
    override val recommendedMaxPlayers = 8
    override val frameIntervalMs = 50L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    data class Tank(
        val playerId: String,
        var x: Float = 0f,
        var y: Float = 0f,
        var hp: Int = 100,
        var dir: Float = 0f
    )

    data class Bullet(
        val ownerId: String,
        var x: Float = 0f,
        var y: Float = 0f,
        var dir: Float = 0f
    )

    private val _tanks = MutableStateFlow<Map<String, Tank>>(emptyMap())
    val tanks: StateFlow<Map<String, Tank>> = _tanks.asStateFlow()

    private val _bullets = MutableStateFlow<List<Bullet>>(emptyList())
    val bullets: StateFlow<List<Bullet>> = _bullets.asStateFlow()

    private var syncJob: kotlinx.coroutines.Job? = null
    private var isHost: Boolean = false

    override fun onPlayerJoin(player: NetworkPlayer) {
        _tanks.update { it + (player.id to Tank(player.id, x = 100f, y = 100f)) }
    }

    override fun onPlayerLeave(playerId: String) {
        _tanks.update { it - playerId }
    }

    override fun onGameStart() {
        // Host 启动 20Hz 游戏循环
        isHost = true
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                updateBullets()
                if (isHost) matchManager.broadcastState(collectGameState())
                delay(frameIntervalMs)
            }
        }
    }

    override fun onGameEnd(reason: String) {
        syncJob?.cancel()
        isHost = false
    }

    override fun onReceiveGameCommand(player: NetworkPlayer, frame: Long, command: JsonObject) {
        val action = command["action"]?.toString()?.trim('"') ?: return
        val tank = _tanks.value[player.id] ?: return
        when (action) {
            "move" -> {
                val dx = command["dx"]?.toString()?.toFloatOrNull() ?: 0f
                val dy = command["dy"]?.toString()?.toFloatOrNull() ?: 0f
                _tanks.update { it + (player.id to tank.copy(x = tank.x + dx, y = tank.y + dy)) }
            }
            "shoot" -> {
                val dir = command["dir"]?.toString()?.toFloatOrNull() ?: 0f
                _bullets.update {
                    it + Bullet(player.id, tank.x, tank.y, dir)
                }
            }
        }
    }

    override fun onReceiveGameState(state: JsonObject, from: String) {
        // 解析远端状态
        val tanksArr = state["tanks"] ?: return
        // 在真实场景中应反序列化为结构化数据；此处仅演示
    }

    override fun collectGameState(): JsonObject = buildJsonObject {
        put("frame", System.currentTimeMillis())
        put("tanks", buildJsonObject {
            _tanks.value.forEach { (id, t) ->
                put(id, buildJsonObject {
                    put("x", t.x)
                    put("y", t.y)
                    put("hp", t.hp)
                    put("dir", t.dir)
                })
            }
        })
        put("bullets", buildJsonArray {
            _bullets.value.forEach { b ->
                addJsonObject {
                    put("owner", b.ownerId)
                    put("x", b.x)
                    put("y", b.y)
                    put("dir", b.dir)
                }
            }
        })
    }

    override fun submitLocalCommand(command: JsonObject) {
        val localId = playerManager.localPlayerId
        val tank = _tanks.value[localId] ?: return
        val action = command["action"]?.toString()?.trim('"') ?: return
        when (action) {
            "move" -> {
                val dx = command["dx"]?.toString()?.toFloatOrNull() ?: 0f
                val dy = command["dy"]?.toString()?.toFloatOrNull() ?: 0f
                _tanks.update { it + (localId to tank.copy(x = tank.x + dx, y = tank.y + dy)) }
            }
            "shoot" -> {
                val dir = command["dir"]?.toString()?.toFloatOrNull() ?: 0f
                _bullets.update { it + Bullet(localId, tank.x, tank.y, dir) }
            }
        }
        matchManager.broadcastCommand(playerManager.getPlayer(localId) ?: return, 0L, command)
    }

    private fun updateBullets() {
        _bullets.update { list ->
            list.mapNotNull { b ->
                val nx = b.x + kotlin.math.cos(Math.toRadians(b.dir.toDouble())).toFloat() * 5f
                val ny = b.y + kotlin.math.sin(Math.toRadians(b.dir.toDouble())).toFloat() * 5f
                if (nx < 0 || nx > 2000 || ny < 0 || ny > 2000) null
                else b.copy(x = nx, y = ny)
            }
        }
    }
}
