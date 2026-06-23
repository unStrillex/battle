# WiFi Battle Platform 架构设计文档

## 1. 总体架构

WiFi Battle Platform 采用 **Clean Architecture + MVVM + 模块化** 设计，分为三个核心模块：

```
┌──────────────────────────────────────────────────────────┐
│                       UI 模块 (Compose)                    │
│  HomeScreen / LobbyScreen / RoomScreen / GameScreen      │
│              +  ViewModel + Theme/Components              │
└──────────────────────────────────────────────────────────┘
                          ▲
                          │  StateFlow / SharedFlow
                          ▼
┌──────────────────────────────────────────────────────────┐
│                    Core 模块 (Framework)                  │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────┐   │
│  │  Network     │ │  Discovery   │ │   Sync         │   │
│  │  Transport   │ │  UDP/NSD     │ │  State/Command │   │
│  └──────────────┘ └──────────────┘ └────────────────┘   │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────┐   │
│  │  Player      │ │  Room        │ │  Protocol      │   │
│  │  Manager     │ │  Manager     │ │  Message       │   │
│  └──────────────┘ └──────────────┘ └────────────────┘   │
└──────────────────────────────────────────────────────────┘
                          ▲
                          │  implements GameAdapter
                          ▼
┌──────────────────────────────────────────────────────────┐
│              Game Adapter 模块 (游戏适配)                  │
│     TankBattleAdapter / ChessAdapter / MobaAdapter       │
│              + GameAdapterRegistry (注册中心)            │
└──────────────────────────────────────────────────────────┘
```

## 2. 模块架构

### 2.1 Core 模块

| 包名 | 职责 |
|------|------|
| `core.network` | TCP 传输、心跳、延迟检测 |
| `core.discovery` | UDP 广播、NSD/mDNS 服务发现 |
| `core.protocol` | 自定义消息协议（JSON） |
| `core.room` | 房间生命周期、状态广播 |
| `core.player` | 玩家加入/离开/准备管理 |
| `core.sync` | 状态同步、Lockstep、RPC |

### 2.2 UI 模块

| 包名 | 职责 |
|------|------|
| `ui.theme` | Material 3 深色科技风主题 |
| `ui.component` | 通用组件（GlowCard、PlayerAvatar、TagBadge） |
| `ui.viewmodel` | AppViewModel（全局单例） |
| `ui.screen` | 各页面（Home / Lobby / Room / Game / Settings） |

### 2.3 Adapter 模块

- 接口：`GameAdapter`
- 注册中心：`GameAdapterRegistry`
- 实现示例：Tank / Chess / Moba

## 3. 网络架构

### 3.1 双通道

```
Host (房主)
  ├─ TCP ServerSocket (port 9999)         ← 可靠连接（房间内通讯）
  └─ UDP Broadcast (port 9998, 255.255.255.255)  ← 房间发现

Client (玩家)
  ├─ TCP Socket → Host
  └─ UDP MulticastSocket 监听
```

### 3.2 服务发现

| 方案 | 兼容性 | 速度 | 触发场景 |
|------|--------|------|----------|
| UDP 广播 | 所有 Android | 中 | 默认 |
| NSD (mDNS) | API 16+ | 快 | 推荐 |

框架默认同时启用两种，合并去重后展示。

### 3.3 消息协议

帧格式（JSON + 换行分隔）：

```json
{
  "type": "game_state",
  "seq": 1234,
  "from": "host-id",
  "to": null,
  "timestamp": 1700000000,
  "payload": { "frame": 100, "tanks": {...} }
}
```

详细类型见 `core/protocol/NetworkMessage.kt`。

## 4. 数据流

### 4.1 创建房间

```
User → HomeScreen
  → AppViewModel.createRoom()
    → RoomManager.createRoom()
    → PlayerManager.addPlayer(local)
    → NetworkTransport.startHost()
    → RoomDiscovery.startBroadcasting()
    → NsdDiscovery.register()
  → 跳转 RoomScreen
```

### 4.2 加入房间

```
User → LobbyScreen
  → AppViewModel.joinDiscoveredRoom(room)
    → NetworkTransport.connect(room.hostIp, room.port)
    → 等待 WELCOME 消息
  → 跳转 RoomScreen
```

### 4.3 游戏中同步

```
Host 端 (MatchManager):
  loop @frameIntervalMs:
    state = adapter.collectGameState()
    transport.broadcast(GAME_STATE, state)

Client 端 (GameAdapter):
  onReceiveGameState(state, from) {
    applyState(state)
  }
```

## 5. 关键技术点

- **协程 + Flow**：所有网络收发与状态更新基于 Kotlin Coroutine & Flow
- **依赖注入**：Hilt 全局单例管理
- **序列化**：kotlinx.serialization
- **UI**：Jetpack Compose + Material 3
- **可扩展**：新游戏只需实现 GameAdapter 并注册
