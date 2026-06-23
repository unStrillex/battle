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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * MOBA 示例 - 5v5
 *
 * 特性：
 *  - 严格 Lockstep
 *  - 每帧 30Hz
 *  - 玩家属性、技能、装备同步
 *  - 演示 RPC（用于主机裁决技能命中）
 */
class MobaAdapter : BaseGameAdapter() {
    override val gameId = "moba"
    override val displayName = "MOBA 对战"
    override val recommendedMaxPlayers = 10
    override val frameIntervalMs = 33L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    data class Hero(
        val playerId: String,
        var x: Float = 0f,
        var y: Float = 0f,
        var hp: Int = 600,
        var mp: Int = 300,
        var level: Int = 1,
        var team: Int = 0 // 0 or 1
    )

    private val _heroes = MutableStateFlow<Map<String, Hero>>(emptyMap())
    val heroes: StateFlow<Map<String, Hero>> = _heroes.asStateFlow()

    private var loopJob: kotlinx.coroutines.Job? = null

    override fun onPlayerJoin(player: NetworkPlayer) {
        val team = (_heroes.value.size) % 2
        val hero = Hero(player.id, x = if (team == 0) 200f else 1800f, y = 1000f, team = team)
        _heroes.update { it + (player.id to hero) }
    }

    override fun onPlayerLeave(playerId: String) {
        _heroes.update { it - playerId }
    }

    override fun onGameStart() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive) {
                // Host 端以 30Hz 广播游戏状态
                matchManager.broadcastState(collectGameState())
                delay(frameIntervalMs)
            }
        }
    }

    override fun onGameEnd(reason: String) {
        loopJob?.cancel()
    }

    override fun onReceiveGameCommand(player: NetworkPlayer, frame: Long, command: JsonObject) {
        val action = command["action"]?.toString()?.trim('"') ?: return
        val hero = _heroes.value[player.id] ?: return
        when (action) {
            "move" -> {
                val tx = command["tx"]?.toString()?.toFloatOrNull() ?: hero.x
                val ty = command["ty"]?.toString()?.toFloatOrNull() ?: hero.y
                _heroes.update { it + (player.id to hero.copy(x = tx, y = ty)) }
            }
            "skill" -> {
                // 通过 RPC 通知主机裁决
                scope.launch {
                    matchManager.callRpc("host", "judge_skill", buildJsonObject {
                        put("playerId", player.id)
                        put("skillId", command["skillId"]?.toString()?.toIntOrNull() ?: 1)
                    })
                }
            }
        }
    }

    override fun collectGameState(): JsonObject = buildJsonObject {
        put("frame", System.currentTimeMillis())
        put("heroes", kotlinx.serialization.json.buildJsonObject {
            _heroes.value.forEach { (id, h) ->
                put(id, buildJsonObject {
                    put("x", h.x); put("y", h.y)
                    put("hp", h.hp); put("mp", h.mp)
                    put("lv", h.level); put("team", h.team)
                })
            }
        })
    }

    override fun submitLocalCommand(command: JsonObject) {
        val localId = playerManager.localPlayerId
        matchManager.broadcastCommand(playerManager.getPlayer(localId) ?: return, 0L, command)
    }

    override fun broadcastGameData(data: JsonObject) {
        // 默认空实现，自定义数据由 GameAdapter 子类按需覆写
    }
}
