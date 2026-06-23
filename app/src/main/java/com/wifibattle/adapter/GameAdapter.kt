package com.wifibattle.adapter

import com.wifibattle.core.player.NetworkPlayerManager
import com.wifibattle.core.room.NetworkRoomManager
import com.wifibattle.core.sync.MatchManager
import com.wifibattle.data.model.NetworkPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * GameAdapter - 游戏适配器接口
 *
 * 设计目标：新游戏接入时 **无需修改核心代码**，
 * 只需实现该接口并通过 [GameAdapterRegistry] 注册即可。
 *
 * 框架负责：
 *   - 网络收发
 *   - 房间/玩家管理
 *   - 同步调度
 *   - 心跳/Ping
 *
 * Adapter 负责：
 *   - 游戏自身逻辑
 *   - 状态序列化/反序列化
 *   - UI 行为
 */
interface GameAdapter {
    /** 游戏唯一标识，用于房间 gameType 字段 */
    val gameId: String

    /** 游戏显示名称 */
    val displayName: String

    /** 推荐最大玩家数 */
    val recommendedMaxPlayers: Int get() = 8

    /** 帧间隔（毫秒），影响同步频率 */
    val frameIntervalMs: Long get() = 50L

    // ---- 生命周期回调 ----
    fun onAttach(
        roomManager: NetworkRoomManager,
        playerManager: NetworkPlayerManager,
        matchManager: MatchManager
    )
    fun onDetach()

    // ---- 玩家事件 ----
    fun onPlayerJoin(player: NetworkPlayer)
    fun onPlayerLeave(playerId: String)
    fun onPlayerReady(player: NetworkPlayer, ready: Boolean)

    // ---- 游戏事件 ----
    fun onGameStart()
    fun onGameEnd(reason: String)

    // ---- 数据接收 ----
    /** 收到来自主机的完整状态同步 */
    fun onReceiveGameState(state: JsonObject, from: String)

    /** 收到来自其他玩家的指令 */
    fun onReceiveGameCommand(player: NetworkPlayer, frame: Long, command: JsonObject)

    /** 收到自定义消息（用于游戏内自定义协议） */
    fun onReceiveGameData(data: JsonObject, from: String)

    /** 收到 RPC 响应 */
    fun onReceiveRpcResult(callId: String, result: JsonElement)

    // ---- 主动调用 ----
    /** Host 端：收集当前游戏状态（用于广播） */
    fun collectGameState(): JsonObject

    /** 玩家提交一条指令 */
    fun submitLocalCommand(command: JsonObject)

    /** 广播自定义数据（不参与 lockstep） */
    fun broadcastGameData(data: JsonObject)
}
