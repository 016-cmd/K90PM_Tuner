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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.k90pm.tuner.ui.theme.Appearance

/**
 * 半透明玻璃卡片 — 纯色半透明 + 细边框模拟玻璃效果.
 * 不使用 Backdrop 库,避免 MIUI 渲染引擎崩溃.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(Appearance.radius.dp)

    val fillColor = glassFill(0f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillColor, shape)
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

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(Appearance.radius.dp)

    val fillColor = glassFill(0f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillColor, shape)
            .border(0.5.dp, borderColor, shape)
    ) {
        content()
    }
}

@Composable
fun GlassSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape((Appearance.radius - 8f).coerceAtLeast(10f).dp)

    val fillColor = glassFill(0.05f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f)
    else Color.Black.copy(alpha = 0.08f)

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
/** 卡片底色：自动跟深浅色；纯白 / 纯黑由外观设置强制 */
@androidx.compose.runtime.Composable
private fun glassFill(extra: Float): Color {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red * 0.299f + cs.background.green * 0.587f + cs.background.blue * 0.114f < 0.5f
    return when (Appearance.tintMode) {
        1 -> Color.White.copy(alpha = (Appearance.cardAlphaLight + extra).coerceAtMost(0.97f))
        2 -> Color.Black.copy(alpha = (Appearance.cardAlphaDark + extra).coerceAtMost(0.95f))
        else -> if (isDark) Color.Black.copy(alpha = (Appearance.cardAlphaDark + extra).coerceAtMost(0.95f))
        else Color.White.copy(alpha = (Appearance.cardAlphaLight + extra).coerceAtMost(0.97f))
    }
}
