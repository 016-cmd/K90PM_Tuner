package com.k90pm.tuner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.cos
import kotlin.math.sin

/**
 * 全局壁纸层 backdrop。
 *
 * Theme 里只有「壁纸那一层」被 layerBackdrop 记录下来，
 * 所有液态玻璃卡片都从这一层采样 —— 卡片自己不在这一层里，
 * 不会出现「卡片采样自己」的递归，也就不会踩 RenderThread 崩溃。
 */
object GlassBackdrop {
    @Volatile
    var instance: Backdrop? = null
}

/**
 * 液态玻璃表面：Backdrop 真折射优先，不可用时退回纯色玻璃。
 *
 * 参数全部由「玻璃风格」档位派生，不额外增加设置项：
 *   0 清透 / 1 标准 / 2 浓厚 / 3 柔和 / 4 省电（省电档关折射）
 */
@Composable
fun Modifier.glassSurface(radius: Float = Appearance.radius): Modifier {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red * 0.299f + cs.background.green * 0.587f +
            cs.background.blue * 0.114f < 0.5f

    val g = Appearance.glass
    val tint = if (isDark) Color.Black else Color.White
    val tintAlpha = if (isDark) Appearance.cardAlphaDark else Appearance.cardAlphaLight

    // 折射强度 / 模糊半径跟着玻璃档位走
    val refr = when (g) {
        0 -> 1.00f   // 清透：折射最明显
        2 -> 0.70f   // 浓厚：模糊多、折射淡
        3 -> 1.20f   // 柔和：折射最柔
        4 -> 0f      // 省电：不做折射
        else -> 1.00f
    }
    val blurDp = when (g) {
        0 -> 0.8f
        2 -> 4.0f
        3 -> 2.2f
        4 -> 0f
        else -> 1.8f
    }
    val vib = when (g) {
        0 -> 0.55f
        2 -> 1.00f
        3 -> 0.75f
        4 -> 0.35f
        else -> 0.80f
    }

    val density = LocalDensity.current
    val rPx = with(density) { radius.dp.toPx() }
    val strokePx = with(density) { 0.9.dp.toPx() }

    val bd = GlassBackdrop.instance
    val useBackdrop = bd != null && g != 4 && android.os.Build.VERSION.SDK_INT >= 33

    if (useBackdrop) {
        val shape = RoundedCornerShape(radius.dp)
        return this
            .drawBackdrop(
                backdrop = bd!!,
                shape = { shape },
                effects = {
                    if (blurDp > 0f) blur(blurDp.dp.toPx())
                    vibrancy()
                    if (g != 4) {
                        lens(
                            (10f + 12f * refr).dp.toPx(),
                            (18f + 30f * refr).dp.toPx(),
                            true
                        )
                    }
                },
                onDrawSurface = {
                    // 底色压薄，让折射与壁纸透出来
                    drawRoundRect(
                        color = tint.copy(alpha = tintAlpha * 0.62f),
                        cornerRadius = CornerRadius(rPx, rPx)
                    )
                }
            )
            .drawWithCache {
                onDrawBehind {
                    val w = size.width
                    val h = size.height
                    val corner = CornerRadius(rPx, rPx)
                    // 顶部亮线（光源方向固定为左上 → 右下）
                    if (vib > 0f) {
                        val rad = Math.toRadians(45.0)
                        val dx = cos(rad).toFloat()
                        val dy = sin(rad).toFloat()
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                0f to Color.White.copy(alpha = 0.04f + 0.16f * vib),
                                0.5f to Color.White.copy(alpha = 0.02f),
                                1f to Color.Transparent,
                                start = Offset(w / 2f - dx * w, h / 2f - dy * h),
                                end = Offset(w / 2f + dx * w, h / 2f + dy * h)
                            ),
                            cornerRadius = corner
                        )
                    }
                    // 外框：浅色淡灰，深色淡白
                    drawRoundRect(
                        color = if (isDark) Color.White.copy(alpha = 0.12f)
                        else Color.Black.copy(alpha = 0.10f),
                        cornerRadius = corner,
                        style = Stroke(strokePx)
                    )
                    // 内侧一圈暗边，增加玻璃厚度
                    drawRoundRect(
                        color = Color.Black.copy(alpha = if (isDark) 0.10f else 0.04f),
                        topLeft = Offset(strokePx, strokePx),
                        size = androidx.compose.ui.geometry.Size(
                            w - strokePx * 2f,
                            h - strokePx * 2f
                        ),
                        cornerRadius = corner,
                        style = Stroke(strokePx * 0.8f)
                    )
                }
            }
    }

    // ── 回退：纯色玻璃 ──
    return this.drawWithCache {
        onDrawBehind {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(rPx, rPx)
            drawRoundRect(
                color = tint.copy(alpha = tintAlpha),
                cornerRadius = corner
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (isDark) 0.10f else 0.30f),
                    0.20f to Color.Transparent
                ),
                cornerRadius = corner
            )
            drawRoundRect(
                color = if (isDark) Color.White.copy(alpha = 0.12f)
                else Color.Black.copy(alpha = 0.10f),
                cornerRadius = corner,
                style = Stroke(strokePx)
            )
        }
    }
}