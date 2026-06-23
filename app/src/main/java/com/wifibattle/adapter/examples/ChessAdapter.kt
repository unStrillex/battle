package com.wifibattle.adapter.examples

import com.wifibattle.adapter.BaseGameAdapter
import com.wifibattle.data.model.NetworkPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 棋牌游戏示例 - 斗地主 / 五子棋 通用
 *
 * 特点：
 *  - 2~6 人
 *  - 同步频率低（仅事件触发）
 *  - 状态同步 + 事件消息
 */
class ChessAdapter : BaseGameAdapter() {
    override val gameId = "chess"
    override val displayName = "棋牌对战"
    override val recommendedMaxPlayers = 6
    override val frameIntervalMs = 200L

    private val _board = MutableStateFlow(emptyList<String>())
    val board: StateFlow<List<String>> = _board.asStateFlow()

    private val _currentTurn = MutableStateFlow<String?>(null)
    val currentTurn: StateFlow<String?> = _currentTurn.asStateFlow()

    override fun onGameStart() {
        // 初始化棋盘
        val players = playerManager.players.value
        _currentTurn.value = players.firstOrNull()?.id
    }

    override fun onReceiveGameCommand(player: NetworkPlayer, frame: Long, command: JsonObject) {
        val type = command["type"]?.toString()?.trim('"') ?: return
        when (type) {
            "play_card" -> {
                val card = command["card"]?.toString()?.trim('"') ?: return
                val list = _board.value.toMutableList()
                list.add("${player.id}:$card")
                _board.value = list
                rotateTurn()
            }
            "pass" -> rotateTurn()
        }
    }

    override fun onReceiveGameState(state: JsonObject, from: String) {
        _board.value = (state["board"]?.toString() ?: "")
            .split(",")
            .filter { it.isNotBlank() }
        _currentTurn.value = state["turn"]?.toString()?.trim('"')
    }

    override fun collectGameState(): JsonObject = buildJsonObject {
        put("board", _board.value.joinToString(","))
        put("turn", _currentTurn.value ?: "")
    }

    override fun submitLocalCommand(command: JsonObject) {
        val localId = playerManager.localPlayerId
        matchManager.broadcastCommand(playerManager.getPlayer(localId) ?: return, 0L, command)
    }

    private fun rotateTurn() {
        val players = playerManager.players.value
        if (players.isEmpty()) return
        val idx = players.indexOfFirst { it.id == _currentTurn.value }
        _currentTurn.value = players[(idx + 1) % players.size].id
    }
}
