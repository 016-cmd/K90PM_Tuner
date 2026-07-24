package com.k90pm.tuner.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.k90pm.tuner.ui.theme.WallpaperState

/**
 * 液态玻璃卡片 — 参照 K90PM Web UI + Scene BlurView 设计。
 * 背景：壁纸模糊版（WallpaperState.blurred）+ 顶部微高光 + 细边框。
 * 无白色填充，真正的毛玻璃质感。
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

    // 边框
    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.08f)

    // 顶部高光（模拟玻璃反射）
    val highlight = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.06f else 0.12f),
            Color.Transparent
        ),
        startY = 0f,
        endY = 80f
    )

    val blurred = WallpaperState.blurred

    Box(
        modifier = modifier
            .clip(shape)
            .border(0.5.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // 模糊壁纸背景
        if (blurred != null) {
            AsyncImage(
                model = blurred,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

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

    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.08f)

    val blurred = WallpaperState.blurred

    Box(
        modifier = modifier
            .clip(shape)
            .border(0.5.dp, borderColor, shape)
    ) {
        if (blurred != null) {
            AsyncImage(
                model = blurred,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}

/**
 * 设置页专用卡片——极淡底色保证可读性
 */
@Composable
fun GlassSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(16.dp)

    val fillColor = if (isDark) Color.White.copy(alpha = 0.03f)
    else Color.White.copy(alpha = 0.50f)

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