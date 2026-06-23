package com.wifibattle.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.wifibattle.ui.component.GlowCard
import com.wifibattle.ui.theme.NeonCyan
import com.wifibattle.ui.theme.NeonPurple
import com.wifibattle.ui.viewmodel.AppViewModel

@Composable
fun SettingsScreen(nav: NavHostController, vm: AppViewModel = hiltViewModel()) {
    val localPlayer by vm.localPlayer.collectAsState()
    var nickname by remember(localPlayer.nickname) { mutableStateOf(localPlayer.nickname) }
    var autoDiscovery by remember { mutableStateOf(true) }
    var showPing by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = NeonCyan)
            }
            Spacer(Modifier.width(8.dp))
            Text("设置", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(16.dp))

        GlowCard(accent = NeonPurple) {
            Text("玩家信息", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    vm.setNickname(it)
                },
                label = { Text("昵称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))

        GlowCard(accent = NeonCyan) {
            Text("网络", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SettingsSwitch("自动搜索房间", autoDiscovery) { autoDiscovery = it }
            SettingsSwitch("显示 Ping", showPing) { showPing = it }
        }
        Spacer(Modifier.height(12.dp))

        GlowCard(accent = NeonPurple) {
            Text("游戏", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SettingsSwitch("开启音效", soundEnabled) { soundEnabled = it }
        }
        Spacer(Modifier.height(24.dp))

        Text(
            "WiFi Battle Platform v1.0.0",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SettingsSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
