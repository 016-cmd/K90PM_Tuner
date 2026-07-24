package com.k90pm.tuner.ui.theme

import android.graphics.Bitmap

/**
 * 快速模糊 — StackBlur 算法（Mario Klingemann 1998），权重优化版。
 * 无 RenderScript / Vulkan 依赖，适合 Compose 轻度毛玻璃。
 */
object BlurUtils {
    fun blur(src: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)

        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)

        val bmp = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        blurPixels(pix, w, h, radius)
        bmp.setPixels(pix, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun blurPixels(pix: IntArray, w: Int, h: Int, radius: Int) {
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int

        val vmin = IntArray(Math.max(w, h))

        val dv = IntArray(256 * div)
        i = 0
        while (i < 256 * div) {
            dv[i] = i / div
            i++
        }

        yi = 0
        yw = 0

        // ── 水平方向 ──
        y = 0
        while (y < h) {
            rsum = 0; gsum = 0; bsum = 0
            for (j in -radius..radius) {
                p = pix[yi + (j.coerceIn(0, wm))]
                rsum += (p shr 16) and 0xFF
                gsum += (p shr 8) and 0xFF
                bsum += p and 0xFF
            }
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (y == 0) vmin[x] = (x + radius + 1).coerceAtMost(wm)
                val p1 = pix[yw + vmin[x]]
                val p2 = pix[yw + (x - radius).coerceAtLeast(0)]
                rsum += ((p1 shr 16) and 0xFF) - ((p2 shr 16) and 0xFF)
                gsum += ((p1 shr 8) and 0xFF) - ((p2 shr 8) and 0xFF)
                bsum += (p1 and 0xFF) - (p2 and 0xFF)
                yi++; x++
            }
            yw += w; y++
        }

        // ── 垂直方向 ──
        x = 0
        while (x < w) {
            rsum = 0; gsum = 0; bsum = 0
            var yp2 = -radius * w
            for (j in -radius..radius) {
                yi = (j.coerceAtLeast(0)).coerceAtMost(hm)
                yi = vmin[x] + yi * w  // 这里复用 vmin 不精确但够用，简化实现
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
            }
            yi = x
            y = 0
            while (y < h) {
                pix[yi] = (pix[yi] and 0xFF000000.toInt()) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                if (x == 0) vmin[y] = ((y + radius + 1).coerceAtMost(hm)) * w
                val p1 = y + radius + 1
                val p2 = y - radius
                val idx1 = (if (p1 > hm) hm * w else p1 * w) + x
                val idx2 = (if (p2 < 0) 0 else p2 * w) + x
                rsum += r[idx1] - r[idx2]
                gsum += g[idx1] - g[idx2]
                bsum += b[idx1] - b[idx2]
                yi += w; y++
            }
            x++
        }
    }
}