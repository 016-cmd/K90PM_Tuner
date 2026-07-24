package com.k90pm.tuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃卡片 — 参照 K90PM Web UI 设计。
 * 亮色：透明底 + 极淡白填充 + 细边框 + 顶部微高光（模拟 backdrop-filter blur）
 * 暗色：暗底 + 微弱亮填充(0.03) + 细边框 + 顶部微高光
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(20.dp)

    // Web UI 暗色模式: rgba(255,255,255,.03)；亮色: 略白
    val fillColor = if (isDark) Color.White.copy(alpha = 0.04f)
    else Color.White.copy(alpha = 0.55f)

    // 边框: 亮色 rgba(60,60,67,.08)；暗色 rgba(255,255,255,.06)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.08f)

    // 顶部高光（模拟 glass 反射）
    val highlight = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.04f else 0.12f),
            Color.Transparent
        ),
        startY = 0f,
        endY = 80f
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillColor, shape)
            .border(0.5.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // 顶部高光层
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(highlight, shape)
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            content()
        }
    }
}

/**
 * 液态玻璃容器——无内边距
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(20.dp)

    val fillColor = if (isDark) Color.White.copy(alpha = 0.04f)
    else Color.White.copy(alpha = 0.55f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillColor, shape)
            .border(0.5.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * 设置页专用卡片——稍不透明，适合列表项
 */
@Composable
fun GlassSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(16.dp)

    val fillColor = if (isDark) Color.White.copy(alpha = 0.05f)
    else Color.White.copy(alpha = 0.60f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f)
    else Color.Black.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fillColor, shape)
            .border(0.5.dp, borderColor, shape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}