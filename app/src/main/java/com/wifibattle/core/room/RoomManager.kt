package com.wifibattle.core.room

import com.wifibattle.core.player.NetworkPlayerManager
import com.wifibattle.data.model.NetworkPlayer
import com.wifibattle.data.model.NetworkRoom
import com.wifibattle.data.model.RoomState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * NetworkRoom 接口
 */
interface NetworkRoomManager {
    val room: StateFlow<NetworkRoom?>
    val roomState: StateFlow<RoomState>
    fun createRoom(name: String, hostId: String, hostName: String, hostIp: String, maxPlayers: Int = 8, gameType: String = "default"): NetworkRoom
    fun joinRoom(room: NetworkRoom, player: NetworkPlayer)
    fun closeRoom()
    fun updateRoom(transform: (NetworkRoom) -> NetworkRoom)
    fun setStarted(started: Boolean)
}

/**
 * RoomManager 默认实现
 */
class DefaultRoomManager(
    private val playerManager: NetworkPlayerManager
) : NetworkRoomManager {

    private val _room = MutableStateFlow<NetworkRoom?>(null)
    override val room: StateFlow<NetworkRoom?> = _room.asStateFlow()

    private val _roomState = MutableStateFlow(RoomState(room = NetworkRoom(
        id = "", name = "", hostId = "", hostName = "", hostIp = "", port = 0
    )))
    override val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    override fun createRoom(
        name: String, hostId: String, hostName: String, hostIp: String,
        maxPlayers: Int, gameType: String
    ): NetworkRoom {
        val r = NetworkRoom(
            id = java.util.UUID.randomUUID().toString(),
            name = name.ifBlank { "${hostName}的房间" },
            hostId = hostId,
            hostName = hostName,
            hostIp = hostIp,
            port = com.wifibattle.core.network.NetworkTransport.DEFAULT_PORT,
            currentPlayers = 1,
            maxPlayers = maxPlayers.coerceIn(2, 16),
            gameType = gameType
        )
        _room.value = r
        _roomState.value = _roomState.value.copy(room = r)
        return r
    }

    override fun joinRoom(room: NetworkRoom, player: NetworkPlayer) {
        _room.value = room
        _roomState.value = _roomState.value.copy(room = room)
        playerManager.addPlayer(player)
    }

    override fun closeRoom() {
        _room.value = null
        playerManager.clear()
    }

    override fun updateRoom(transform: (NetworkRoom) -> NetworkRoom) {
        _room.update { current -> current?.let(transform) }
        _room.value?.let { r -> _roomState.update { it.copy(room = r) } }
    }

    override fun setStarted(started: Boolean) {
        _roomState.update { it.copy(isStarted = started) }
    }
}
