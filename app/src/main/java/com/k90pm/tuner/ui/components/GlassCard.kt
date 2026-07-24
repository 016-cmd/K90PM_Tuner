package com.k90pm.tuner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * iOS 26 液态玻璃风格卡片。
 * - 半透明背景 + 模糊
 * - 微妙边框高光
 * - 斜向渐变光晕
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.35f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f)
    val gradientStart = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.5f)
    val gradientEnd = if (isDark) Color.White.copy(alpha = 0.01f) else Color.White.copy(alpha = 0.15f)

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(gradientStart, gradientEnd),
                    start = Offset(Float.POSITIVE_INFINITY, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
            .border(0.5.dp, borderColor, shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            content()
        }
    }
}

/**
 * 液态玻璃容器——仅外观无内容 padding
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.35f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            .background(bgColor)
    ) {
        content()
    }
}