# 游戏接入指南

本指南演示如何将一个新的游戏接入 WiFi Battle Platform 框架。

## 1. 接入流程概览

```
1. 实现 GameAdapter 接口（继承 BaseGameAdapter 即可）
2. 在 Application 或 Hilt 模块中注册
3. （可选）在 UI 中添加游戏选择入口
4. 完成！
```

新游戏接入 **无需修改任何核心代码**。

## 2. 案例一：棋牌游戏

完整代码见 `app/src/main/java/com/wifibattle/adapter/examples/ChessAdapter.kt`

```kotlin
class ChessAdapter : BaseGameAdapter() {
    override val gameId = "chess"
    override val displayName = "棋牌对战"
    override val recommendedMaxPlayers = 6

    private val _board = MutableStateFlow<List<String>>(emptyList())
    val board: StateFlow<List<String>> = _board.asStateFlow()

    override fun onGameStart() {
        _currentTurn.value = playerManager.players.value.firstOrNull()?.id
    }

    override fun onReceiveGameCommand(player: NetworkPlayer, frame: Long, command: JsonObject) {
        // 处理玩家出牌
    }

    override fun collectGameState(): JsonObject = buildJsonObject {
        put("board", _board.value.joinToString(","))
        put("turn", _currentTurn.value ?: "")
    }
}
```

注册：

```kotlin
GameAdapterRegistry.register(ChessAdapter())
```

## 3. 案例二：坦克大战

完整代码见 `app/src/main/java/com/wifibattle/adapter/examples/TankBattleAdapter.kt`

```kotlin
class TankBattleAdapter : BaseGameAdapter() {
    override val gameId = "tank_battle"
    override val displayName = "坦克大战"
    override val recommendedMaxPlayers = 8
    override val frameIntervalMs = 50L  // 20Hz

    private val _tanks = MutableStateFlow<Map<String, Tank>>(emptyMap())
    val tanks: StateFlow<Map<String, Tank>> = _tanks.asStateFlow()

    override fun onGameStart() {
        // 启动游戏循环
    }

    override fun submitLocalCommand(command: JsonObject) {
        val localId = playerManager.localPlayerId
        // 1. 本地预测
        // 2. 通过 matchManager 广播给 Host
        matchManager.broadcastCommand(
            playerManager.getPlayer(localId)!!, 0L, command
        )
    }
}
```

## 4. 案例三：MOBA

完整代码见 `app/src/main/java/com/wifibattle/adapter/examples/MobaAdapter.kt`

```kotlin
class MobaAdapter : BaseGameAdapter() {
    override val gameId = "moba"
    override val displayName = "MOBA 对战"
    override val recommendedMaxPlayers = 10
    override val frameIntervalMs = 33L  // 30Hz

    override fun onGameStart() {
        // 30Hz 状态广播循环
    }

    override fun onReceiveGameCommand(player: NetworkPlayer, frame: Long, command: JsonObject) {
        when (command["action"]?.toString()?.trim('"')) {
            "move" -> { /* ... */ }
            "skill" -> {
                // RPC：让 Host 裁决技能命中
                scope.launch {
                    matchManager.callRpc("host", "judge_skill", buildJsonObject {
                        put("playerId", player.id)
                        put("skillId", 1)
                    })
                }
            }
        }
    }
}
```

## 5. 同步策略选择

| 游戏类型 | 同步策略 | 推荐 frameInterval |
|----------|----------|-------------------|
| 棋牌 | 状态同步 (低频) | 200ms |
| 休闲竞技 | 状态同步 | 50-100ms |
| 射击 / MOBA | Lockstep + 状态 | 33-50ms |
| 塔防 | 状态同步 | 100ms |

## 6. 自定义协议

通过 `broadcastGameData` / `onReceiveGameData` 收发任意 JSON：

```kotlin
override fun broadcastGameData(data: JsonObject) {
    matchManager.broadcastCommand(localPlayer, 0L, data)
}

override fun onReceiveGameData(data: JsonObject, from: String) {
    val type = data["type"].toString().trim('"')
    when (type) {
        "card_played" -> { /* ... */ }
        "item_picked" -> { /* ... */ }
    }
}
```

## 7. RPC 高级用法

注册自定义方法（Host 端）：

```kotlin
matchManager.registerRpc("judge_skill") { args ->
    val skillId = args["skillId"]?.toString()?.toIntOrNull() ?: 1
    val targetId = args["targetId"]?.toString()?.trim('"') ?: ""
    // 业务逻辑：判定命中
    buildJsonObject {
        put("hit", true)
        put("damage", 100)
    }
}
```

调用：

```kotlin
val result = matchManager.callRpc("host", "judge_skill", buildJsonObject {
    put("skillId", 1)
    put("targetId", "player-2")
})
```
