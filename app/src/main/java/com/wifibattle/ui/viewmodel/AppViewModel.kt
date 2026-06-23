package com.wifibattle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifibattle.adapter.GameAdapter
import com.wifibattle.adapter.GameAdapterRegistry
import com.wifibattle.core.discovery.NsdDiscovery
import com.wifibattle.core.discovery.RoomDiscovery
import com.wifibattle.core.network.NetworkTransport
import com.wifibattle.core.player.DefaultPlayerManager
import com.wifibattle.core.room.DefaultRoomManager
import com.wifibattle.core.sync.DefaultMatchManager
import com.wifibattle.data.model.NetworkPlayer
import com.wifibattle.data.model.NetworkRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 共享 ViewModel - 持有全局服务（网络、房间、玩家、同步）
 *
 * Hilt 在 Application 作用域下提供单例，所有页面共享同一份状态。
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    val transport: NetworkTransport,
    val roomDiscovery: RoomDiscovery,
    val nsdDiscovery: NsdDiscovery,
    val playerManager: DefaultPlayerManager,
    val roomManager: DefaultRoomManager,
    val matchManager: DefaultMatchManager
) : ViewModel() {

    // 当前用户信息
    private val _localPlayer = MutableStateFlow(
        NetworkPlayer(
            id = playerManager.localPlayerId,
            nickname = "Player-${playerManager.localPlayerId.take(4)}",
            isHost = false
        )
    )
    val localPlayer: StateFlow<NetworkPlayer> = _localPlayer.asStateFlow()

    val players: StateFlow<List<NetworkPlayer>> = playerManager.players
    val room: StateFlow<NetworkRoom?> = roomManager.room
    val discoveredRooms: StateFlow<List<NetworkRoom>> = roomDiscovery.discoveredRooms
    val nsdRooms: StateFlow<List<NetworkRoom>> = nsdDiscovery.discoveredRooms

    fun setNickname(nickname: String) {
        _localPlayer.value = _localPlayer.value.copy(nickname = nickname.ifBlank { "Player" })
    }

    /** 创建房间并启动 Host */
    fun createRoom(name: String, maxPlayers: Int, gameType: String) {
        val ip = roomDiscovery.getLocalIpAddress() ?: "0.0.0.0"
        val room = roomManager.createRoom(
            name = name,
            hostId = _localPlayer.value.id,
            hostName = _localPlayer.value.nickname,
            hostIp = ip,
            maxPlayers = maxPlayers,
            gameType = gameType
        )
        playerManager.addPlayer(_localPlayer.value.copy(isHost = true))
        transport.startHost(NetworkTransport.DEFAULT_PORT)
        roomDiscovery.startBroadcasting(room)
        nsdDiscovery.register(room, NetworkTransport.DEFAULT_PORT)
    }

    /** 关闭房间 */
    fun closeRoom() {
        roomDiscovery.stopBroadcasting()
        nsdDiscovery.unregister()
        transport.stop()
        roomManager.closeRoom()
    }

    /** 通过 IP 加入房间 */
    fun joinRoomByIp(ip: String, port: Int = NetworkTransport.DEFAULT_PORT) {
        transport.connect(ip, port)
    }

    /** 从自动发现列表加入 */
    fun joinDiscoveredRoom(room: NetworkRoom) {
        transport.connect(room.hostIp, room.port)
        // 简单等待 WELCOME 后由 protocol 触发
    }

    /** 开始搜索 */
    fun startSearching() {
        roomDiscovery.startListening()
        nsdDiscovery.startDiscovery()
    }

    fun stopSearching() {
        roomDiscovery.stopListening()
        nsdDiscovery.stopDiscovery()
    }

    /** 设置本地玩家准备状态 */
    fun setReady(ready: Boolean) {
        playerManager.setReady(_localPlayer.value.id, ready)
    }

    /** 房主开始游戏 */
    fun startGame() {
        matchManager.startMatch()
        roomManager.setStarted(true)
    }

    /** 加载游戏 Adapter */
    fun loadGameAdapter(gameId: String): GameAdapter? {
        val adapter = GameAdapterRegistry.get(gameId) ?: return null
        adapter.onAttach(roomManager, playerManager, matchManager)
        return adapter
    }

    override fun onCleared() {
        super.onCleared()
        transport.stop()
        roomDiscovery.shutdown()
        nsdDiscovery.unregister()
        nsdDiscovery.stopDiscovery()
    }
}
