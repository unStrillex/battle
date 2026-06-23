package com.wifibattle.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifibattle.ui.theme.NeonCyan
import com.wifibattle.ui.theme.NeonPurple

/**
 * 渐变发光卡片 - 游戏大厅通用组件
 */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "glow")
    val alpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Card(
        modifier = modifier
            .drawBehind {
                val brush = Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = alpha * 0.4f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                drawRect(brush)
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(accent.copy(alpha = 0.6f), accent.copy(alpha = 0.1f))),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/**
 * 玩家头像组件
 */
@Composable
fun PlayerAvatar(
    name: String,
    isHost: Boolean = false,
    isReady: Boolean = false,
    modifier: Modifier = Modifier
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val ringColor = when {
        isHost -> NeonPurple
        isReady -> NeonCyan
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(width = 2.dp, color = ringColor, shape = RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

/**
 * 标签小徽章
 */
@Composable
fun TagBadge(
    text: String,
    color: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
