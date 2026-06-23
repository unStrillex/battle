package com.wifibattle.ui.screen.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.wifibattle.R
import com.wifibattle.data.model.NetworkRoom
import com.wifibattle.ui.component.GlowCard
import com.wifibattle.ui.component.TagBadge
import com.wifibattle.ui.theme.NeonCyan
import com.wifibattle.ui.theme.NeonPurple
import com.wifibattle.ui.viewmodel.AppViewModel

@Composable
fun LobbyScreen(nav: NavHostController, vm: AppViewModel = hiltViewModel()) {
    val rooms by vm.discoveredRooms.collectAsState()
    val nsd by vm.nsdRooms.collectAsState()

    LaunchedEffect(Unit) {
        vm.startSearching()
    }

    // 合并去重
    val merged = remember(rooms, nsd) {
        (rooms + nsd).distinctBy { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                vm.stopSearching()
                nav.popBackStack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = NeonCyan)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.lobby_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                vm.stopSearching()
                vm.startSearching()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = NeonCyan)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 通过 IP 加入
        IpJoinCard { ip, port ->
            vm.joinRoomByIp(ip, port)
            nav.navigate("room")
        }

        Spacer(Modifier.height(16.dp))
        Text("发现的房间 (${merged.size})", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (merged.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.msg_searching),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "请确保所有设备在同一 WiFi 下",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(merged, key = { it.id }) { room ->
                    RoomListItem(room) {
                        vm.joinDiscoveredRoom(room)
                        nav.navigate("room")
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomListItem(room: NetworkRoom, onClick: () -> Unit) {
    GlowCard(accent = if (room.isFull) NeonPurple else NeonCyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(room.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text("房主：${room.hostName}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row {
                    TagBadge("${room.currentPlayers}/${room.maxPlayers}", color = NeonCyan)
                    Spacer(Modifier.width(6.dp))
                    TagBadge("IP ${room.hostIp}", color = NeonPurple)
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onClick,
                enabled = !room.isFull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(if (room.isFull) "已满" else "加入")
            }
        }
    }
}

@Composable
private fun IpJoinCard(onJoin: (String, Int) -> Unit) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("9999") }
    GlowCard(accent = NeonPurple) {
        Text("通过 IP 加入", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                placeholder = { Text("192.168.1.x") },
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() } },
                placeholder = { Text("9999") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onJoin(ip, port.toIntOrNull() ?: 9999) },
            enabled = ip.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text("连接")
        }
    }
}
