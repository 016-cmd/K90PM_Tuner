package com.k90pm.tuner.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.k90pm.tuner.ui.theme.WallpaperState

/**
 * 液态玻璃卡片。
 * 浅色：白底高透 + 模糊壁纸背景 + 细边框
 * 深色：黑底高透 + 模糊壁纸背景 + 细边框
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

    // 底色：浅色白底高透 / 深色黑底高透
    val fillColor = if (isDark) Color.Black.copy(alpha = 0.35f)
    else Color.White.copy(alpha = 0.55f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f)
    else Color.Black.copy(alpha = 0.06f)

    val blurredBmp = remember { WallpaperState.blurred }

    Box(
        modifier = modifier
            .clip(shape)
            .border(0.5.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // 底层：模糊壁纸全填充
        if (blurredBmp != null) {
            androidx.compose.foundation.Image(
                bitmap = blurredBmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // 上层：半透明底色
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fillColor, shape)
        )
        // 内容
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

    val fillColor = if (isDark) Color.Black.copy(alpha = 0.35f)
    else Color.White.copy(alpha = 0.55f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f)
    else Color.Black.copy(alpha = 0.06f)

    val blurredBmp = remember { WallpaperState.blurred }

    Box(
        modifier = modifier
            .clip(shape)
            .border(0.5.dp, borderColor, shape)
    ) {
        if (blurredBmp != null) {
            androidx.compose.foundation.Image(
                bitmap = blurredBmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fillColor, shape)
        )
        content()
    }
}

/**
 * 设置页专用卡片——同样风格
 */
@Composable
fun GlassSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(16.dp)

    val fillColor = if (isDark) Color.Black.copy(alpha = 0.40f)
    else Color.White.copy(alpha = 0.60f)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f)
    else Color.Black.copy(alpha = 0.08f)

    val blurredBmp = remember { WallpaperState.blurred }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(0.5.dp, borderColor, shape)
    ) {
        if (blurredBmp != null) {
            androidx.compose.foundation.Image(
                bitmap = blurredBmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fillColor, shape)
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}