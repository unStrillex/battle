package com.wifibattle.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 网络消息通用协议（自定义消息协议）
 *
 * 帧格式：
 *   [type: MessageType] [seq: Long] [from: String] [to: String?] [payload: JsonObject]
 *
 * 所有跨设备通讯都使用此结构序列化，保证可扩展性。
 */
@Serializable
data class NetworkMessage(
    val type: MessageType,
    val seq: Long = 0L,
    val from: String = "",
    val to: String? = null,    // null = 广播
    val timestamp: Long = System.currentTimeMillis(),
    val payload: JsonObject = JsonObject(emptyMap())
) {
    @Serializable
    enum class MessageType {
        // ---- 房间握手 ----
        @SerialName("hello") HELLO,                 // 客户端 -> 主机：握手
        @SerialName("welcome") WELCOME,             // 主机 -> 客户端：欢迎 + 房间状态
        @SerialName("ping") PING,
        @SerialName("pong") PONG,

        // ---- 房间管理 ----
        @SerialName("player_join") PLAYER_JOIN,
        @SerialName("player_leave") PLAYER_LEAVE,
        @SerialName("player_ready") PLAYER_READY,
        @SerialName("room_update") ROOM_UPDATE,
        @SerialName("room_close") ROOM_CLOSE,

        // ---- 聊天 ----
        @SerialName("chat") CHAT,

        // ---- 游戏 ----
        @SerialName("game_start") GAME_START,
        @SerialName("game_state") GAME_STATE,        // 状态同步
        @SerialName("game_command") GAME_COMMAND,    // 指令同步 (Lockstep)
        @SerialName("game_rpc") GAME_RPC,            // RPC
        @SerialName("game_end") GAME_END,
        @SerialName("game_data") GAME_DATA,          // 自定义数据

        // ---- 错误 ----
        @SerialName("error") ERROR
    }
}

/**
 * 通用响应包装（用于 RPC 风格调用）
 */
@Serializable
data class RpcEnvelope(
    val callId: String,
    val method: String,
    val args: JsonObject = JsonObject(emptyMap()),
    val result: JsonElement? = null,
    val error: String? = null
)
