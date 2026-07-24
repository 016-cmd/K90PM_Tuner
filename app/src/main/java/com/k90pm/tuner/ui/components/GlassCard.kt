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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * iOS 26 液态玻璃卡片 — Scene 风格简化版。
 * 半透明 surface 色 + 圆角 + 0.5dp 边框。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    // 仿 Scene: surface 色 + alpha
    val bgAlpha = if (isDark) 0.25f else 0.5f
    val borderColor = colors.outlineVariant.copy(alpha = if (isDark) 0.4f else 0.6f)
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface.copy(alpha = bgAlpha))
            .border(0.5.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            content()
        }
    }
}

/**
 * 液态玻璃容器——纯外观无 padding
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val bgAlpha = if (isDark) 0.25f else 0.5f
    val borderColor = colors.outlineVariant.copy(alpha = if (isDark) 0.4f else 0.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            .background(colors.surface.copy(alpha = bgAlpha))
    ) {
        content()
    }
}