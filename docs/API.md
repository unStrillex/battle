# API 参考文档

## 1. Core API

### 1.1 `NetworkTransport`

**包**：`com.wifibattle.core.network`

#### 方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `startHost(port: Int = 9999)` | port: 监听端口 | Boolean | 启动 Host |
| `connect(host: String, port: Int = 9999)` | host: 目标 IP, port | Boolean | 作为 Client 连接 |
| `broadcast(message: NetworkMessage)` | message | Unit | 广播消息 |
| `sendTo(address: String, message: NetworkMessage)` | address: 连接标识, message | Unit | 发送给指定连接 |
| `stop()` | - | Unit | 停止服务 |

#### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `messages` | `SharedFlow<NetworkMessage>` | 收到消息流 |
| `isRunning` | `StateFlow<Boolean>` | 是否运行中 |

### 1.2 `RoomDiscovery` (UDP)

**包**：`com.wifibattle.core.discovery`

| 方法 | 说明 |
|------|------|
| `startBroadcasting(room: NetworkRoom)` | Host：开始广播房间 |
| `stopBroadcasting()` | 停止广播 |
| `startListening()` | Client：开始监听 |
| `stopListening()` | 停止监听 |
| `discoveredRooms: StateFlow<List<NetworkRoom>>` | 发现的房间 |
| `getLocalIpAddress(): String?` | 获取本机局域网 IP |

### 1.3 `NsdDiscovery` (mDNS)

**包**：`com.wifibattle.core.discovery`

| 方法 | 说明 |
|------|------|
| `register(room: NetworkRoom, port: Int)` | Host：注册服务 |
| `unregister()` | 注销 |
| `startDiscovery()` | Client：浏览服务 |
| `stopDiscovery()` | 停止浏览 |

### 1.4 `NetworkRoomManager`

**包**：`com.wifibattle.core.room`

| 方法 | 说明 |
|------|------|
| `createRoom(name, hostId, hostName, hostIp, maxPlayers, gameType): NetworkRoom` | 创建 |
| `joinRoom(room, player)` | 加入 |
| `closeRoom()` | 关闭 |
| `updateRoom(transform)` | 更新 |
| `setStarted(started)` | 标记开始 |
| `room: StateFlow<NetworkRoom?>` | 当前房间 |
| `roomState: StateFlow<RoomState>` | 完整状态 |

### 1.5 `NetworkPlayerManager`

**包**：`com.wifibattle.core.player`

| 方法 | 说明 |
|------|------|
| `addPlayer(player)` | 添加 |
| `removePlayer(playerId)` | 移除 |
| `updatePlayer(playerId, transform)` | 更新 |
| `getPlayer(playerId)` | 查询 |
| `setReady(playerId, ready)` | 设置准备 |
| `isAllReady()` | 是否全部就绪 |
| `clear()` | 清空 |
| `localPlayerId: String` | 本机 ID |
| `players: StateFlow<List<NetworkPlayer>>` | 玩家列表 |

### 1.6 `MatchManager`

**包**：`com.wifibattle.core.sync`

| 方法 | 说明 |
|------|------|
| `startMatch()` | 开始 |
| `endMatch(reason)` | 结束 |
| `broadcastState(state)` | 广播状态 |
| `broadcastCommand(player, frame, command)` | 广播指令 |
| `callRpc(target, method, args)` | RPC 调用 |

## 2. Adapter API

### 2.1 `GameAdapter` 接口

| 方法 | 说明 |
|------|------|
| `onAttach(roomManager, playerManager, matchManager)` | 绑定框架 |
| `onDetach()` | 解绑 |
| `onPlayerJoin(player)` | 玩家加入 |
| `onPlayerLeave(playerId)` | 玩家离开 |
| `onPlayerReady(player, ready)` | 准备状态 |
| `onGameStart()` | 游戏开始 |
| `onGameEnd(reason)` | 游戏结束 |
| `onReceiveGameState(state, from)` | 收到状态 |
| `onReceiveGameCommand(player, frame, command)` | 收到指令 |
| `onReceiveGameData(data, from)` | 自定义数据 |
| `onReceiveRpcResult(callId, result)` | RPC 返回 |
| `collectGameState()` | 收集状态（Host） |
| `submitLocalCommand(command)` | 提交本地指令 |
| `broadcastGameData(data)` | 广播自定义数据 |

### 2.2 `GameAdapterRegistry`

| 方法 | 说明 |
|------|------|
| `register(adapter)` | 注册 |
| `unregister(gameId)` | 注销 |
| `get(gameId)` | 获取 |
| `all()` | 全部 |
| `clear()` | 清空 |

## 3. 数据模型

### 3.1 `NetworkPlayer`

```kotlin
data class NetworkPlayer(
    val id: String,
    val nickname: String,
    val avatarUrl: String = "",
    val isReady: Boolean = false,
    val isHost: Boolean = false,
    val pingMs: Long = 0L,
    val joinedAt: Long = ...
)
```

### 3.2 `NetworkRoom`

```kotlin
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
    val createdAt: Long = ...
)
```

### 3.3 `NetworkMessage`

```kotlin
data class NetworkMessage(
    val type: MessageType,
    val seq: Long = 0L,
    val from: String = "",
    val to: String? = null,
    val timestamp: Long = ...,
    val payload: JsonObject = JsonObject(emptyMap())
)
```

## 4. 消息类型

| Type | 方向 | 用途 |
|------|------|------|
| HELLO | C→H | 客户端握手 |
| WELCOME | H→C | 房间状态 |
| PING / PONG | 双向 | 心跳 |
| PLAYER_JOIN | 广播 | 玩家加入 |
| PLAYER_LEAVE | 广播 | 玩家离开 |
| PLAYER_READY | 广播 | 准备状态 |
| ROOM_UPDATE | 广播 | 房间信息更新 |
| CHAT | 广播 | 聊天 |
| GAME_START | H→C | 游戏开始 |
| GAME_STATE | H→C | 状态同步 |
| GAME_COMMAND | C→H | 指令同步 |
| GAME_RPC | 双向 | RPC |
| GAME_END | 广播 | 结束 |
| ERROR | 双向 | 错误 |
