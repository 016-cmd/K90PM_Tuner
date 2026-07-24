package com.k90pm.tuner.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.k90pm.tuner.ui.theme.WallpaperState
import kotlin.math.max
import kotlin.math.min

/**
 * 液态玻璃卡片 — Scene h41.m() 同款精确裁剪模糊。
 *
 * 原理：
 * 1. 壁纸 → 192px宽 → RenderScript模糊 → WallpaperState.blurred + scale
 * 2. onGloballyPositioned + positionInWindow → 窗口坐标
 * 3. 按 scale 从模糊壁纸精确裁剪 → drawBehind 绘制
 * 4. 上层半透明白底/黑底 + 细边框 = 液态玻璃质感
 */

/** 对每个卡片从 WallpaperState.blurred 裁剪模糊背景的共用工具 */
@Composable
private fun rememberGlassModifier(
    shape: RoundedCornerShape,
    fillColor: Color,
    borderColor: Color,
): Modifier {
    // 缓存 key = (尺寸, 位置) — 二者任一变化即重新裁剪（Scene h41.m() 同款语义）
    val cachedBlur = remember { mutableStateOf<Pair<Pair<Size, Pair<Float, Float>>, Bitmap>?>(null) }

    return Modifier
        .clip(shape)
        .onGloballyPositioned { coords ->
            val blurred = WallpaperState.blurred ?: return@onGloballyPositioned
            val scale = WallpaperState.scale
            if (scale <= 0f) return@onGloballyPositioned

            val pos = coords.positionInWindow()
            val cardW = coords.size.width.toFloat()
            val cardH = coords.size.height.toFloat()
            if (cardW <= 0 || cardH <= 0) return@onGloballyPositioned

            val cardSize = Size(cardW, cardH)
            val cardPos = Pair(pos.x, pos.y)
            val cacheKey = Pair(cardSize, cardPos)
            if (cachedBlur.value?.first == cacheKey) return@onGloballyPositioned

            // Scene 同款算法: x = iArr[0] / width, y = iArr[1] / width
            val x = (pos.x / scale).toInt()
            val y = (pos.y / scale).toInt()
            val w = (cardW / scale).toInt()
            val h = (cardH / scale).toInt()

            val sx = max(0, x)
            val sy = max(0, y)
            val sw = min(blurred.width - sx, w)
            val sh = min(blurred.height - sy, h)
            if (sw <= 0 || sh <= 0) return@onGloballyPositioned

            try {
                val cropped = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(cropped)
                canvas.drawBitmap(
                    blurred,
                    Rect(sx, sy, sx + sw, sy + sh),
                    Rect(0, 0, sw, sh),
                    null
                )
                cachedBlur.value?.second?.recycle()
                cachedBlur.value = Pair(cacheKey, cropped)
            } catch (_: Exception) {}
        }
        .drawBehind {
            val pair = cachedBlur.value ?: return@drawBehind
            val bmp = pair.second
            if (bmp.isRecycled) return@drawBehind
            drawContext.canvas.nativeCanvas.drawBitmap(
                bmp,
                Rect(0, 0, bmp.width, bmp.height),
                Rect(0, 0, size.width.toInt(), size.height.toInt()),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
        }
        .background(fillColor, shape)
        .border(0.5.dp, borderColor, shape)
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

    val glassMod = rememberGlassModifier(shape, fillColor, borderColor)

    Box(
        modifier = modifier
            .then(glassMod)
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

    val glassMod = rememberGlassModifier(shape, fillColor, borderColor)

    Box(modifier = modifier.then(glassMod)) {
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

    val glassMod = rememberGlassModifier(shape, fillColor, borderColor)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(glassMod)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}