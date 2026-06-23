package com.wifibattle.ui.screen.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.wifibattle.adapter.GameAdapter
import com.wifibattle.ui.component.PlayerAvatar
import com.wifibattle.ui.component.TagBadge
import com.wifibattle.ui.theme.NeonCyan
import com.wifibattle.ui.theme.NeonPink
import com.wifibattle.ui.theme.NeonPurple
import com.wifibattle.ui.viewmodel.AppViewModel
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun GameScreen(nav: NavHostController, gameId: String, vm: AppViewModel = hiltViewModel()) {
    val players by vm.players.collectAsState()
    val localPlayer by vm.localPlayer.collectAsState()

    // 加载对应游戏的 Adapter
    val adapter = remember(gameId) { vm.loadGameAdapter(gameId) }

    // 通用游戏视图：根据 Adapter 类型分发
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部网络状态
        TopBar(localPlayer.nickname, onBack = { nav.popBackStack() })

        // 玩家状态条
        PlayersBar(players, localPlayer.id)

        // 游戏画布（通用示例）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF050810))
        ) {
            GameCanvas()
        }

        // 控制按钮（示例 - 坦克大战）
        if (gameId == "tank_battle" || gameId == "moba") {
            GameControlPanel(adapter)
        } else {
            // 棋牌类只显示提示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("等待出牌中…", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TopBar(name: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = NeonCyan)
        }
        Spacer(Modifier.width(8.dp))
        Text(name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        TagBadge("Ping 32ms", color = NeonCyan)
        Spacer(Modifier.width(6.dp))
        TagBadge("●", color = NeonPink)
    }
}

@Composable
private fun PlayersBar(players: List<com.wifibattle.data.model.NetworkPlayer>, localId: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        players.take(8).forEach { p ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlayerAvatar(p.nickname, p.isHost, p.isReady)
                Spacer(Modifier.height(4.dp))
                Text(p.nickname.take(6), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                if (p.id == localId) {
                    Text("你", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameCanvas() {
    // 通用游戏画布示例 - 网格 + 简单动画
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 40f
        val w = size.width
        val h = size.height

        // 网格
        var x = 0f
        while (x < w) {
            drawLine(
                color = Color(0xFF1A2030),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
            x += gridSize
        }
        var y = 0f
        while (y < h) {
            drawLine(
                color = Color(0xFF1A2030),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += gridSize
        }

        // 中心装饰
        drawCircle(
            color = NeonCyan.copy(alpha = 0.1f),
            radius = 80f,
            center = Offset(w / 2, h / 2)
        )
        drawCircle(
            color = NeonCyan,
            radius = 6f,
            center = Offset(w / 2, h / 2)
        )
    }
}

@Composable
private fun GameControlPanel(adapter: GameAdapter?) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ControlButton("↑") {
                    adapter?.submitLocalCommand(buildJsonObject { put("action", "move"); put("dx", 0f); put("dy", -8f) })
                }
                ControlButton("●") {
                    adapter?.submitLocalCommand(buildJsonObject { put("action", "shoot"); put("dir", 0f) })
                }
                ControlButton("↓") {
                    adapter?.submitLocalCommand(buildJsonObject { put("action", "move"); put("dx", 0f); put("dy", 8f) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ControlButton("←") {
                    adapter?.submitLocalCommand(buildJsonObject { put("action", "move"); put("dx", -8f); put("dy", 0f) })
                }
                ControlButton("Q") {
                    adapter?.submitLocalCommand(buildJsonObject { put("action", "skill"); put("skillId", 1) })
                }
                ControlButton("→") {
                    adapter?.submitLocalCommand(buildJsonObject { put("action", "move"); put("dx", 8f); put("dy", 0f) })
                }
            }
        }
    }
}

@Composable
private fun ControlButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = NeonCyan.copy(alpha = 0.2f),
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}
