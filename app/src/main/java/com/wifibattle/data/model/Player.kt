package com.wifibattle.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 玩家信息
 * @param id 玩家唯一 ID（设备 UUID）
 * @param nickname 玩家昵称
 * @param avatarUrl 头像地址
 * @param isReady 是否已准备
 * @param isHost 是否为房主
 * @param pingMs 网络延迟（毫秒）
 * @param joinedAt 加入时间戳
 */
@Serializable
data class NetworkPlayer(
    val id: String,
    val nickname: String,
    val avatarUrl: String = "",
    val isReady: Boolean = false,
    val isHost: Boolean = false,
    val pingMs: Long = 0L,
    val joinedAt: Long = System.currentTimeMillis()
)

/**
 * 房间信息（用于广播 & 列表显示）
 */
@Serializable
data class NetworkRoom(
    val id: String,
    val name: String,
    val hostId: String,
    val hostName: String,
    val hostIp: String,
    val port: Int,
    val currentPlayers: Int,
    val maxPlayers: Int = 16,
    val gameType: String = "default",
    val passwordProtected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isFull: Boolean get() = currentPlayers >= maxPlayers
}

/**
 * 房间内状态（包含玩家列表与设置）
 */
@Serializable
data class RoomState(
    val room: NetworkRoom,
    val players: List<NetworkPlayer> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val isStarted: Boolean = false,
    val gameData: String = ""
)

/**
 * 聊天消息
 */
@Serializable
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text" // text | system | event
)
