package com.wifibattle.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
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
import com.wifibattle.ui.component.GlowCard
import com.wifibattle.ui.component.TagBadge
import com.wifibattle.ui.theme.NeonCyan
import com.wifibattle.ui.theme.NeonPink
import com.wifibattle.ui.theme.NeonPurple
import com.wifibattle.ui.viewmodel.AppViewModel

@Composable
fun HomeScreen(nav: NavHostController, vm: AppViewModel = hiltViewModel()) {
    val localPlayer by vm.localPlayer.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp)
        ) {
            // 顶部栏
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { nav.navigate("settings") }) {
                    Icon(Icons.Default.Settings, contentDescription = "设置", tint = NeonCyan)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 玩家卡片
            GlowCard(accent = NeonPurple) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = localPlayer.nickname.firstOrNull()?.uppercase()?.toString() ?: "P",
                            color = NeonPurple,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(localPlayer.nickname, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(
                            "ID: ${localPlayer.id.take(8)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TagBadge("在线", color = NeonCyan)
                }
            }

            Spacer(Modifier.height(32.dp))

            // 创建房间
            BigActionButton(
                title = "创建房间",
                subtitle = "成为房主，邀请好友",
                icon = Icons.Default.Add,
                accent = NeonCyan,
                onClick = {
                    vm.createRoom(
                        name = "${localPlayer.nickname}的房间",
                        maxPlayers = 8,
                        gameType = "tank_battle"
                    )
                    nav.navigate("room")
                }
            )

            Spacer(Modifier.height(16.dp))

            // 搜索房间
            BigActionButton(
                title = "搜索房间",
                subtitle = "自动发现局域网房间",
                icon = Icons.Default.Search,
                accent = NeonPurple,
                onClick = {
                    vm.startSearching()
                    nav.navigate("lobby")
                }
            )

            Spacer(Modifier.weight(1f))

            // 底部状态
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("局域网已就绪", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                TagBadge("v1.0.0", color = NeonPink)
            }
        }
    }
}

@Composable
private fun BigActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}


