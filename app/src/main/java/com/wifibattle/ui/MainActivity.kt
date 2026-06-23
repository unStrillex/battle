package com.wifibattle.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wifibattle.adapter.GameAdapterRegistry
import com.wifibattle.adapter.examples.ChessAdapter
import com.wifibattle.adapter.examples.MobaAdapter
import com.wifibattle.adapter.examples.TankBattleAdapter
import com.wifibattle.ui.screen.game.GameScreen
import com.wifibattle.ui.screen.home.HomeScreen
import com.wifibattle.ui.screen.lobby.LobbyScreen
import com.wifibattle.ui.screen.room.RoomScreen
import com.wifibattle.ui.screen.settings.SettingsScreen
import com.wifibattle.ui.theme.WiFiBattleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 注册示例游戏 Adapter（实际项目可在 Application 中根据云端配置动态注册）
        GameAdapterRegistry.register(TankBattleAdapter())
        GameAdapterRegistry.register(ChessAdapter())
        GameAdapterRegistry.register(MobaAdapter())
        setContent {
            WiFiBattleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }
}

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("lobby") { LobbyScreen(nav) }
        composable("room") { RoomScreen(nav) }
        composable("game/{gameId}") { entry ->
            val gameId = entry.arguments?.getString("gameId") ?: "tank_battle"
            GameScreen(nav, gameId)
        }
        composable("settings") { SettingsScreen(nav) }
    }
}
