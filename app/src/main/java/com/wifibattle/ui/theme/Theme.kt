package com.wifibattle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 科技感深色主题 - Material Design 3
 */
private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonPink = Color(0xFFFF4081)
private val DarkBg = Color(0xFF0B0F1A)
private val DarkSurface = Color(0xFF121826)
private val DarkSurfaceHigh = Color(0xFF1B2236)
private val OnDark = Color(0xFFE3E8F4)
private val OnDarkMuted = Color(0xFF8C95B0)

private val DarkColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF005A6E),
    onPrimaryContainer = Color(0xFFB8F1FF),
    secondary = NeonPurple,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4A2A8E),
    onSecondaryContainer = Color(0xFFEADDFF),
    tertiary = NeonPink,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = OnDarkMuted,
    outline = Color(0xFF3A4358),
    outlineVariant = Color(0xFF2A3044),
    error = Color(0xFFFF5252),
    onError = Color.Black
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.5.sp)
)

@Composable
fun WiFiBattleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 始终使用深色主题（科技感 / 游戏大厅风）
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
