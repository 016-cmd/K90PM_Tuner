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

    /** 屏幕物理宽度 / 192（Scene 同款缩放比），用于坐标换算 */
    @Volatile
    var scale: Float = 1f
        private set

    fun set(ctx: Context, bitmap: Bitmap, dark: Boolean, screenWidthPx: Int) {
        original?.recycle()
        blurred?.recycle()
        original = bitmap
        blurred = BlurUtils.blur(ctx, bitmap, radius = 16f)
        isDark = dark
        // Scene: float width = rootView.getWidth() / 192
        scale = if (blurred != null && blurred!!.width > 0) {
            screenWidthPx.toFloat() / blurred!!.width.toFloat()
        } else 1f
    }

    fun clear() {
        original?.recycle()
        blurred?.recycle()
        original = null
        blurred = null
        scale = 1f
    }
}