package com.k90pm.tuner.ui.theme

import android.content.Context
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

    fun set(ctx: Context, bitmap: Bitmap, dark: Boolean) {
        original?.recycle()
        blurred?.recycle()
        original = bitmap
        blurred = BlurUtils.blur(ctx, bitmap, radius = 16f)
        isDark = dark
    }

    fun clear() {
        original?.recycle()
        blurred?.recycle()
        original = null
        blurred = null
    }
}