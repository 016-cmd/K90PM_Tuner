package com.k90pm.tuner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.k90pm.tuner.ui.theme.WallpaperState
import kotlin.math.roundToInt

/**
 * 液态玻璃卡片 — 全屏模糊壁纸层 + clipToBounds。
 *
 * 原理：
 * 1. 卡片内部放一张全屏模糊壁纸（同 WallpaperState.blurred）
 * 2. 用 offset 按卡片窗口坐标将模糊图平移到正确位置
 * 3. clip + clipToBounds 确保模糊图只在卡片区域内可见
 * 4. 上层半透明白底/黑底 + 细边框
 *
 * 滚动时 offset 自动跟随，天然实时，零裁剪成本。
 */

@Composable
private fun GlassBlurBackground(
    cardShape: RoundedCornerShape,
    fillColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val blurred = WallpaperState.blurred
    // 卡片在窗口中的坐标，用于将全屏模糊图平移到正确位置
    val cardWindowPos = androidx.compose.runtime.mutableStateOf(IntOffset.Zero)

    Box(
        modifier = modifier
            .clip(cardShape)
            .clipToBounds()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                cardWindowPos.value = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
            }
    ) {
        // 全屏模糊壁纸层 —— 通过负 offset 把图移到卡片在窗口中的位置
        if (blurred != null) {
            Image(
                bitmap = blurred.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    // 往上/左偏移卡片窗口坐标的量，使模糊壁纸和窗口对齐
                    .offset { IntOffset(-cardWindowPos.value.x, -cardWindowPos.value.y) },
                contentScale = ContentScale.FillBounds
            )
        }
        // 半透明白底/黑底
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(fillColor, cardShape)
                .border(0.5.dp, borderColor, cardShape)
        )
        // 内容
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(20.dp)

    val fillColor = if (isDark) Color.Black.copy(alpha = 0.20f)
    else Color.White.copy(alpha = 0.35f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.06f)

    GlassBlurBackground(
        cardShape = shape,
        fillColor = fillColor,
        borderColor = borderColor,
        modifier = modifier
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
    val shape = RoundedCornerShape(20.dp)

    val fillColor = if (isDark) Color.Black.copy(alpha = 0.20f)
    else Color.White.copy(alpha = 0.35f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.06f)

    GlassBlurBackground(cardShape = shape, fillColor = fillColor, borderColor = borderColor, modifier = modifier) {
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
    val shape = RoundedCornerShape(16.dp)

    val fillColor = if (isDark) Color.Black.copy(alpha = 0.25f)
    else Color.White.copy(alpha = 0.40f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f)
    else Color.Black.copy(alpha = 0.08f)

    GlassBlurBackground(
        cardShape = shape,
        fillColor = fillColor,
        borderColor = borderColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}