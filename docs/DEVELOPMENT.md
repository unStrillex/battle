# 二次开发指南

## 1. 如何新增游戏

1. 在 `adapter/examples/` 目录下创建 `MyGameAdapter.kt`
2. 继承 `BaseGameAdapter`
3. 实现业务方法
4. 在 `MainActivity.onCreate()` 中注册：

```kotlin
GameAdapterRegistry.register(MyGameAdapter())
```

## 2. 如何扩展协议

### 2.1 新增消息类型

修改 `core/protocol/NetworkMessage.kt` 的 `MessageType` 枚举：

```kotlin
enum class MessageType {
    // ...
    @SerialName("my_event") MY_EVENT
}
```

### 2.2 在 MatchManager 中处理

修改 `core/sync/MatchManager.kt` 的 `handleMessage()`：

```kotlin
NetworkMessage.MessageType.MY_EVENT -> {
    val data = msg.payload
    // 派发到所有 Adapter
    GameAdapterRegistry.all().forEach { it.onReceiveGameData(data, msg.from) }
}
```

## 3. 如何扩展 UI

### 3.1 新增页面

1. 在 `ui/screen/` 下创建 `MyScreen.kt`
2. 在 `MainActivity.kt` 的 `NavHost` 中注册路由：

```kotlin
composable("my_screen") { MyScreen(nav) }
```

3. 在任意页面跳转：

```kotlin
nav.navigate("my_screen")
```

### 3.2 新增组件

在 `ui/component/` 下创建，参考 `CommonComponents.kt` 中的 `GlowCard` / `PlayerAvatar`。

### 3.3 修改主题

修改 `ui/theme/Theme.kt` 的颜色与字体。

## 4. 如何扩展网络层

如果需要支持更多发现协议（如 BLE、WiFi Direct），可以：

1. 创建新类实现 `Discovery` 接口
2. 在 `AppViewModel` 中注入并启用
3. 合并结果到 `discoveredRooms` 中

## 5. 数据库使用

框架已预置 Room（`androidx.room`）。在 `data/local/` 下添加：

```kotlin
@Database(entities = [MyEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun myDao(): MyDao
}
```

在 Hilt 模块中提供：

```kotlin
@Provides @Singleton
fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "wfb.db").build()
```

## 6. 性能优化建议

- **大房间广播**：状态变化时增量同步（diff），避免每帧全量
- **序列化**：避免嵌套复杂对象，使用 `@Serializable` data class
- **协程作用域**：使用 viewModelScope / SupervisorJob 防止泄漏
- **Compose**：使用 `derivedStateOf` / `remember` 减少重组
- **网络**：心跳频率可调（默认 3s），生产可降至 5-10s

## 7. 调试技巧

### 7.1 查看网络日志

```kotlin
Log.d("NetworkTransport", "→ $message")
```

### 7.2 使用 Stetho 抓包

集成 `facebook/stetho` + `okhttp` 拦截器。

### 7.3 真机联调

1. 两台 Android 设备连接同一 WiFi
2. 一台点击「创建房间」
3. 另一台点击「搜索房间」
4. 在房间内发送聊天 / 准备 / 开始

## 8. 常见问题

**Q: 搜索不到房间？**
A: 检查是否同一 WiFi；某些路由器开启 AP 隔离会阻断广播，请关闭。

**Q: 状态不同步？**
A: 检查 Adapter 的 `collectGameState` 与 `onReceiveGameState` 实现是否匹配。

**Q: 如何实现断线重连？**
A: 框架已支持 `NetworkTransport.connect()`，业务侧处理重连退避即可。
