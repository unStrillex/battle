package com.wifibattle.core.sync

import com.wifibattle.data.model.NetworkPlayer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 状态同步接口
 *
 * 适合 RTS、棋牌等需要持续同步完整状态的场景。
 * 实现方负责将游戏状态序列化为 JSON 元素进行广播。
 */
interface GameStateSync {
    /** 当前完整的游戏状态 */
    val currentState: StateFlow<JsonObject?>

    /**
     * 由 Host 调用：周期性地把当前状态广播给所有 Client
     * @param intervalMs 同步频率（默认 50ms = 20Hz）
     */
    fun startSync(intervalMs: Long = 50L)

    fun stopSync()

    /**
     * 由 Client 调用：处理来自主机的状态更新
     * @param state 状态 JSON
     * @param from 发送方 ID（房主）
     */
    fun applyState(state: JsonObject, from: String)
}

/**
 * 指令同步接口 (Lockstep)
 *
 * 适合需要确定性回放的对战类游戏（如 MOBA、RTS、坦克大战）。
 * 所有玩家在第 N 帧执行相同的指令集合，保证一致性。
 */
interface CommandSync {
    /** 当前帧号 */
    val currentFrame: StateFlow<Long>

    /**
     * 添加本地指令（将由框架收集并广播）
     * @param player 发出指令的玩家
     * @param frame 指令所在帧
     * @param command 指令内容
     */
    fun submitCommand(player: NetworkPlayer, frame: Long, command: JsonObject)

    /**
     * 由 Host 调用：广播已确认的指令集合
     */
    fun broadcastCommands(frame: Long, commands: List<Pair<NetworkPlayer, JsonObject>>)

    /**
     * 由 Client 调用：执行远端指令
     */
    fun executeCommands(frame: Long, commands: List<Pair<NetworkPlayer, JsonObject>>)
}

/**
 * RPC 远程调用接口
 */
interface RpcSync {
    /**
     * 调用远端方法
     * @param target 目标玩家 ID，null 表示所有客户端
     * @param method 方法名
     * @param args 参数
     * @return 异步结果
     */
    suspend fun call(target: String?, method: String, args: JsonObject): JsonElement?

    /**
     * 注册本地方法，供远端调用
     */
    fun register(method: String, handler: suspend (JsonObject) -> JsonElement)
}

/**
 * MatchManager - 单场对局管理
 *
 * 封装一局游戏的生命周期与同步逻辑。
 * 框架自动处理连接、收发；具体游戏只需实现 GameAdapter。
 */
interface MatchManager {
    val isMatchRunning: StateFlow<Boolean>

    /** 开始一场对局（Host 端） */
    fun startMatch()

    /** 结束对局 */
    fun endMatch(reason: String = "completed")

    /** 广播游戏状态 */
    fun broadcastState(state: JsonObject)

    /** 广播游戏指令 */
    fun broadcastCommand(player: NetworkPlayer, frame: Long, command: JsonObject)

    /** 调用 RPC */
    suspend fun callRpc(target: String?, method: String, args: JsonObject): JsonElement?
}
