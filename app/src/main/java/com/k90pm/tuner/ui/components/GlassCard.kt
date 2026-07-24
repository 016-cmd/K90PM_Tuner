package com.k90pm.tuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * iOS 26 液态玻璃卡片。
 * - 使用当前主题 surface 色 + 半透明
 * - 斜向渐变高光（左上角亮 → 右下角暗）
 * - 0.5dp 可见边框
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    // 背景：surface 色层 + 半透明
    val bgAlpha = if (isDark) 0.30f else 0.60f
    val baseBg = colors.surface.copy(alpha = bgAlpha)

    // 渐变叠加：左上角更亮
    val highlightColor = if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.White.copy(alpha = 0.6f)
    val shadowColor = Color.White.copy(alpha = 0f)

    // 边框：表面色变体的微妙描边
    val borderColor = if (isDark)
        colors.outlineVariant.copy(alpha = 0.5f)
    else
        colors.outlineVariant.copy(alpha = 0.7f)

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseBg)
            .background(
                Brush.linearGradient(
                    colors = listOf(highlightColor, shadowColor),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
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
 * 液态玻璃容器——纯外观，无内容 padding
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val bgAlpha = if (isDark) 0.30f else 0.60f
    val borderColor = if (isDark)
        colors.outlineVariant.copy(alpha = 0.5f)
    else
        colors.outlineVariant.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            .background(colors.surface.copy(alpha = bgAlpha))
    ) {
        content()
    }
}