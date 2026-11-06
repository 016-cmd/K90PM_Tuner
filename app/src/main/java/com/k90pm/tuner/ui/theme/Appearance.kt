package com.k90pm.tuner.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.min

/**
 * 外观参数 + 壁纸取色。
 *
 * 原则：强调色、次色、第三色全部从当前壁纸派生，换壁纸一起变；
 * 纯黑 / 纯白 / 低饱和壁纸必须有兜底色，绝不允许返回空列表。
 */
object Appearance {

    private const val PREFS = "k90pm_tuner"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── 壁纸派生色 ──
    var accent by mutableStateOf(Color(0xFF22C55E)); private set
    var secondary by mutableStateOf(Color(0xFF5B9BD5)); private set
    var tertiary by mutableStateOf(Color(0xFF8B5CF6)); private set

    /** 壁纸平均亮度（0 暗 ~ 1 亮），用于自动黑白文字与增强可读性 */
    var wallLum by mutableFloatStateOf(0.5f); private set

    // ── 玻璃与交互参数 ──
    /** 0 清透 / 1 标准 / 2 浓厚 / 3 柔和 / 4 省电 */
    var glass by mutableIntStateOf(1); private set
    /** 卡片圆角 12–34dp */
    var radius by mutableFloatStateOf(26f); private set
    /** 果冻按压 0 关 1 开 */
    /** 果冻按压强度 0 / 40 / 70 / 100 */
    var jelly by mutableIntStateOf(70); private set
    /** 可读性 0 默认 / 1 增强 */
    var readLevel by mutableIntStateOf(0); private set
    /** 卡片底色 0 自动 / 1 纯白 / 2 纯黑 */
    var tintMode by mutableIntStateOf(0); private set
    /** 壁纸压暗 0 / 20 / 40 / 60 */
    var wallpaperDim by mutableIntStateOf(0); private set
    /** 页面切换动画 */
    var animPage by mutableIntStateOf(1); private set
    /** 卡片入场动画 */
    var animCard by mutableIntStateOf(1); private set

    fun load(ctx: Context) {
        val p = sp(ctx)
        glass = p.getInt("ap_glass", 1)
        radius = p.getFloat("ap_radius", 26f)
        jelly = p.getInt("ap_jelly", 70)
        readLevel = p.getInt("ap_read", 0)
        tintMode = p.getInt("ap_tint", 0)
        wallpaperDim = p.getInt("ap_wpdim", 0)
        animPage = p.getInt("ap_anim_page", 1)
        animCard = p.getInt("ap_anim_card", 1)
    }

    fun setGlass(ctx: Context, v: Int) {
        glass = v; sp(ctx).edit().putInt("ap_glass", v).apply()
    }

    fun setRadius(ctx: Context, v: Float) {
        radius = v; sp(ctx).edit().putFloat("ap_radius", v).apply()
    }

    fun setJelly(ctx: Context, strength: Int) {
        jelly = strength
        sp(ctx).edit().putInt("ap_jelly", strength).apply()
    }

    fun setTintMode(ctx: Context, v: Int) {
        tintMode = v; sp(ctx).edit().putInt("ap_tint", v).apply()
    }

    fun setWallpaperDim(ctx: Context, v: Int) {
        wallpaperDim = v; sp(ctx).edit().putInt("ap_wpdim", v).apply()
    }

    fun setAnimPage(ctx: Context, on: Boolean) {
        animPage = if (on) 1 else 0
        sp(ctx).edit().putInt("ap_anim_page", animPage).apply()
    }

    fun setAnimCard(ctx: Context, on: Boolean) {
        animCard = if (on) 1 else 0
        sp(ctx).edit().putInt("ap_anim_card", animCard).apply()
    }

    fun setReadLevel(ctx: Context, v: Int) {
        readLevel = v; sp(ctx).edit().putInt("ap_read", v).apply()
    }

    /** 玻璃档位对应的卡片底色透明度 */
    val cardAlphaDark: Float get() = when (glass) {
        0 -> 0.34f      // 清透
        2 -> 0.72f      // 浓厚
        3 -> 0.58f      // 柔和
        4 -> 0.86f      // 省电
        else -> 0.58f   // 标准
    }

    val cardAlphaLight: Float get() = when (glass) {
        0 -> 0.60f
        2 -> 0.90f
        3 -> 0.80f
        4 -> 0.96f
        else -> 0.80f
    }

    /** 从壁纸采样三色。bitmap 为空时用兜底色。 */
    fun applyColors(bitmap: Bitmap?, dark: Boolean) {
        if (bitmap == null) {
            fallback(dark)
            return
        }
        val w = 40
        val h = 40
        val small = runCatching { Bitmap.createScaledBitmap(bitmap, w, h, true) }.getOrNull()
        if (small == null) {
            fallback(dark)
            return
        }
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        if (small !== bitmap) small.recycle()

        var sum = 0.0
        val buckets = HashMap<Int, Int>()
        val hsv = FloatArray(3)
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            sum += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            // 太灰 / 太暗 / 太亮的像素不参与取色
            if (hsv[1] < 0.16f || hsv[2] < 0.15f || hsv[2] > 0.95f) continue
            val key = (hsv[0].toInt() / 12) * 100 + (hsv[1] * 10).toInt()
            buckets[key] = (buckets[key] ?: 0) + 1
        }
        wallLum = (sum / pixels.size.coerceAtLeast(1)).toFloat()

        val picked = ArrayList<Color>()
        buckets.entries.sortedByDescending { it.value }.take(12).forEach { e ->
            val hue = (e.key / 100) * 12f + 6f
            val sat = ((e.key % 100) / 10f).coerceIn(0.34f, 0.92f)
            val v = if (dark) 0.80f else 0.62f
            val c = Color(
                android.graphics.Color.HSVToColor(
                    floatArrayOf(hue, sat, v)
                )
            )
            if (c.luminance() < 0.06f || c.luminance() > 0.96f) return@forEach
            if (picked.none { hueGap(it, c) < 0.16f }) picked.add(c)
        }
        if (picked.isEmpty()) {
            fallback(dark)
            return
        }
        accent = picked[0]
        secondary = picked.getOrElse(1) { shiftHue(picked[0], 26f) }
        tertiary = picked.getOrElse(2) { shiftHue(picked[0], -26f) }
    }

    fun fallback(dark: Boolean) {
        accent = if (dark) Color(0xFF4ADE80) else Color(0xFF16A34A)
        secondary = if (dark) Color(0xFF60A5FA) else Color(0xFF2563EB)
        tertiary = if (dark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
        wallLum = if (dark) 0.12f else 0.92f
    }

    // ── 内置壁纸 ──
    fun builtinAsset(kind: Int): String? = when (kind) {
        1 -> "wp_zzz.jpg"
        2 -> "wp_sea.jpg"
        3 -> "wp_buzz.jpg"
        else -> null
    }

    fun builtinName(kind: Int): String = when (kind) {
        1 -> "绝区零"
        2 -> "海风"
        3 -> "微醺"
        else -> "本地图片"
    }

    private fun hsvOf(c: Color): FloatArray {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(
                (c.red * 255f).toInt().coerceIn(0, 255),
                (c.green * 255f).toInt().coerceIn(0, 255),
                (c.blue * 255f).toInt().coerceIn(0, 255)
            ), out
        )
        return out
    }

    private fun hueGap(a: Color, b: Color): Float {
        val ha = hsvOf(a)[0]
        val hb = hsvOf(b)[0]
        val d = abs(ha - hb)
        return min(d, 360f - d) / 360f
    }

    private fun shiftHue(c: Color, deg: Float): Color {
        val hsv = hsvOf(c)
        hsv[0] = (hsv[0] + deg + 360f) % 360f
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}

/**
 * 小字 / 副标题色。
 * 默认：跟主题但保证 alpha ≥ 0.86；增强：100% 不透明纯黑或纯白，按壁纸亮度反向。
 */
@Composable
fun subInk(alpha: Float = 0.86f): Color {
    val dark = WallpaperState.isDark
    return if (Appearance.readLevel >= 1) {
        if (dark) Color.White else Color.Black
    } else {
        val base = if (dark) Color(0xFFDCDCE4) else Color(0xFF3C3C46)
        base.copy(alpha = alpha.coerceAtLeast(0.80f))
    }
}

/** 主文字色：增强档同样给满不透明纯色 */
@Composable
fun mainInk(): Color {
    val dark = WallpaperState.isDark
    return if (Appearance.readLevel >= 1) {
        if (dark) Color.White else Color.Black
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}
