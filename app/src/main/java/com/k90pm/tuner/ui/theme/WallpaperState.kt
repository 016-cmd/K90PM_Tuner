package com.k90pm.tuner.ui.theme

import android.graphics.Bitmap

/**
 * 全局壁纸状态 — Scene 同款设计：单例持有原始壁纸和轻度模糊版。
 * GlassCard 读取 [blurred] 实现毛玻璃背景。
 */
object WallpaperState {
    @Volatile
    var original: Bitmap? = null
        private set

    @Volatile
    var blurred: Bitmap? = null
        private set

    @Volatile
    var isDark: Boolean = false
        private set

    fun set(bitmap: Bitmap, dark: Boolean) {
        // 缩放原图到 192px 宽（Scene 同款），减少模糊运算量
        val scaled = scaleToWidth(bitmap, 192)
        original?.recycle()
        blurred?.recycle()
        original = scaled
        blurred = BlurUtils.blur(scaled, radius = 8)
        isDark = dark
    }

    fun clear() {
        original?.recycle()
        blurred?.recycle()
        original = null
        blurred = null
    }

    private fun scaleToWidth(src: Bitmap, targetWidth: Int): Bitmap {
        val ratio = targetWidth.toFloat() / src.width
        val h = (src.height * ratio).toInt()
        return Bitmap.createScaledBitmap(src, targetWidth, h.coerceAtLeast(1), true)
    }
}