package com.wifibattle.core.player

import com.wifibattle.data.model.NetworkPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * NetworkPlayer 接口
 *
 * 玩家管理的抽象。新游戏可以继承该接口做扩展。
 */
interface NetworkPlayerManager {
    val players: StateFlow<List<NetworkPlayer>>
    val localPlayerId: String
    fun addPlayer(player: NetworkPlayer)
    fun removePlayer(playerId: String)
    fun updatePlayer(playerId: String, transform: (NetworkPlayer) -> NetworkPlayer)
    fun getPlayer(playerId: String): NetworkPlayer?
    fun setReady(playerId: String, ready: Boolean)
    fun isAllReady(): Boolean
    fun clear()
}

/**
 * 玩家管理器默认实现
 */
class DefaultPlayerManager : NetworkPlayerManager {

    override val localPlayerId: String = UUID.randomUUID().toString()

    private val _players = MutableStateFlow<List<NetworkPlayer>>(emptyList())
    override val players: StateFlow<List<NetworkPlayer>> = _players.asStateFlow()

    override fun addPlayer(player: NetworkPlayer) {
        _players.update { current ->
            if (current.any { it.id == player.id }) current else current + player
        }
    }

    override fun removePlayer(playerId: String) {
        _players.update { it.filterNot { p -> p.id == playerId } }
    }

    override fun updatePlayer(playerId: String, transform: (NetworkPlayer) -> NetworkPlayer) {
        _players.update { list ->
            list.map { if (it.id == playerId) transform(it) else it }
        }
    }

    override fun getPlayer(playerId: String): NetworkPlayer? =
        _players.value.firstOrNull { it.id == playerId }

    override fun setReady(playerId: String, ready: Boolean) {
        updatePlayer(playerId) { it.copy(isReady = ready) }
    }

    override fun isAllReady(): Boolean {
        val list = _players.value
        if (list.size < 2) return false
        return list.all { it.isReady || it.isHost }
    }

    override fun clear() {
        _players.value = emptyList()
    }
}
