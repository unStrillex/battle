package com.wifibattle.adapter

import android.util.Log
import com.wifibattle.core.player.NetworkPlayerManager
import com.wifibattle.core.room.NetworkRoomManager
import com.wifibattle.core.sync.MatchManager
import com.wifibattle.data.model.NetworkPlayer
import kotlinx.serialization.json.JsonObject

/**
 * GameAdapter 基类 - 提供默认空实现
 *
 * 子类只需覆写自己关心的方法。
 */
abstract class BaseGameAdapter : GameAdapter {
    protected lateinit var roomManager: NetworkRoomManager
        private set
    protected lateinit var playerManager: NetworkPlayerManager
        private set
    protected lateinit var matchManager: MatchManager
        private set

    override fun onAttach(
        roomManager: NetworkRoomManager,
        playerManager: NetworkPlayerManager,
        matchManager: MatchManager
    ) {
        this.roomManager = roomManager
        this.playerManager = playerManager
        this.matchManager = matchManager
        Log.i(TAG, "Adapter attached: $gameId")
    }

    override fun onDetach() {
        Log.i(TAG, "Adapter detached: $gameId")
    }

    override fun onPlayerJoin(player: NetworkPlayer) {}
    override fun onPlayerLeave(playerId: String) {}
    override fun onPlayerReady(player: NetworkPlayer, ready: Boolean) {}
    override fun onGameStart() {}
    override fun onGameEnd(reason: String) {}
    override fun onReceiveGameState(state: JsonObject, from: String) {}
    override fun onReceiveGameCommand(player: NetworkPlayer, frame: Long, command: JsonObject) {}
    override fun onReceiveGameData(data: JsonObject, from: String) {}
    override fun onReceiveRpcResult(callId: String, result: kotlinx.serialization.json.JsonElement) {}
    override fun collectGameState(): JsonObject = JsonObject(emptyMap())
    override fun submitLocalCommand(command: JsonObject) {}
    override fun broadcastGameData(data: JsonObject) {}

    companion object { private const val TAG = "BaseGameAdapter" }
}

/**
 * GameAdapter 注册表
 *
 * 启动时通过反射或手动注册。每种游戏注册一个 adapter。
 */
object GameAdapterRegistry {
    private val adapters = linkedMapOf<String, GameAdapter>()

    fun register(adapter: GameAdapter) {
        adapters[adapter.gameId] = adapter
    }

    fun unregister(gameId: String) {
        adapters.remove(gameId)
    }

    fun get(gameId: String): GameAdapter? = adapters[gameId]

    fun all(): List<GameAdapter> = adapters.values.toList()

    fun clear() = adapters.clear()
}
