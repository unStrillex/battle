package com.wifibattle.ui.screen.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.wifibattle.adapter.GameAdapterRegistry
import com.wifibattle.data.model.NetworkPlayer
import com.wifibattle.data.model.NetworkRoom
import com.wifibattle.ui.component.GlowCard
import com.wifibattle.ui.component.PlayerAvatar
import com.wifibattle.ui.component.TagBadge
import com.wifibattle.ui.theme.NeonCyan
import com.wifibattle.ui.theme.NeonPurple
import com.wifibattle.ui.viewmodel.AppViewModel

@Composable
fun RoomScreen(nav: NavHostController, vm: AppViewModel = hiltViewModel()) {
    val room by vm.room.collectAsState()
    val players by vm.players.collectAsState()
    val localPlayer by vm.localPlayer.collectAsState()
    val isHost = localPlayer.isHost

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TopBar(nav) { vm.closeRoom() }
        Spacer(Modifier.height(12.dp))

        RoomInfoCard(room)
        Spacer(Modifier.height(12.dp))

        // 游戏类型选择
        GameTypeSelector(currentGame = room?.gameType ?: "tank_battle", isHost = isHost) { game ->
            // 在真实场景中调用 vm.setGameType(game)
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // 玩家列表
            Box(modifier = Modifier.weight(1f)) {
                PlayersList(players, isHost)
            }
            Spacer(Modifier.width(12.dp))
            // 聊天
            Box(modifier = Modifier.weight(1f)) {
                ChatPanel()
            }
        }

        Spacer(Modifier.height(12.dp))

        BottomBar(
            isHost = isHost,
            allReady = players.size > 1 && players.all { it.isReady || it.isHost },
            onReady = { vm.setReady(true) },
            onCancel = { vm.setReady(false) },
            onStart = {
                vm.startGame()
                val gameId = room?.gameType ?: "tank_battle"
                nav.navigate("game/$gameId")
            }
        )
    }
}

@Composable
private fun TopBar(nav: NavHostController, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onClose(); nav.popBackStack() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = NeonCyan)
        }
        Spacer(Modifier.width(8.dp))
        Text("房间", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.weight(1f))
        TagBadge("局域网", color = NeonCyan)
    }
}

@Composable
private fun RoomInfoCard(room: NetworkRoom?) {
    GlowCard(accent = NeonPurple) {
        Text(room?.name ?: "—", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text("房主：${room?.hostName ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row {
            TagBadge("IP ${room?.hostIp ?: "—"}", color = NeonCyan)
            Spacer(Modifier.width(6.dp))
            TagBadge("端口 ${room?.port ?: 9999}", color = NeonCyan)
        }
    }
}

@Composable
private fun GameTypeSelector(currentGame: String, isHost: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val games = GameAdapterRegistry.all()
    val current = games.firstOrNull { it.gameId == currentGame }
    GlowCard(accent = NeonCyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("游戏类型：", color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            AssistChip(
                onClick = { if (isHost) expanded = true },
                label = { Text(current?.displayName ?: currentGame) },
                colors = AssistChipDefaults.assistChipColors(containerColor = NeonCyan.copy(alpha = 0.2f))
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                games.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g.displayName) },
                        onClick = { onChange(g.gameId); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayersList(players: List<NetworkPlayer>, isHost: Boolean) {
    GlowCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth()) {
        Text("玩家 (${players.size})", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        players.forEach { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerAvatar(p.nickname, p.isHost, p.isReady)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(p.nickname, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    Text("ID: ${p.id.take(6)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                if (p.isHost) TagBadge("房主", color = NeonPurple)
                else if (p.isReady) TagBadge("已准备", color = NeonCyan)
                else TagBadge("等待", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChatPanel() {
    val list = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()

    LaunchedEffect(Unit) {
        list.add("[系统] 欢迎来到房间！")
    }

    GlowCard(accent = NeonPurple, modifier = Modifier.fillMaxWidth()) {
        Text("聊天", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            state = scrollState
        ) {
            items(list) { msg ->
                Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("说点什么…") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (input.isNotBlank()) {
                    list.add("[我] $input")
                    input = ""
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "发送", tint = NeonCyan)
            }
        }
    }
}

@Composable
private fun BottomBar(
    isHost: Boolean,
    allReady: Boolean,
    onReady: () -> Unit,
    onCancel: () -> Unit,
    onStart: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (isHost) {
            Button(
                onClick = onStart,
                enabled = allReady,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("开始游戏")
            }
        } else {
            var ready by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    ready = !ready
                    if (ready) onReady() else onCancel()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ready) NeonCyan else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (ready) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(if (ready) "取消准备" else "准备")
            }
        }
    }
}
