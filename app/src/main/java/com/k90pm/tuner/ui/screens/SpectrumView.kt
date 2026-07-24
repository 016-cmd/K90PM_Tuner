package com.k90pm.tuner.ui.screens

import android.media.audiofx.Visualizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * 频谱特效 — 通过 root 获取当前音频 sessionId，用 Visualizer 捕获 FFT
 * 有音频时动态频谱，无音频时静态底柱 + RGB 流光
 */
@Composable
fun SpectrumView(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(20.dp)
    val fillColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.40f)

    var fftBytes by remember { mutableStateOf(ByteArray(0)) }
    var hue by remember { mutableStateOf(0f) }
    var hasData by remember { mutableStateOf(false) }
    var sessionLabel by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var visualizer: Visualizer? = null

            // 持续尝试获取 session
            while (isActive) {
                try {
                    // 释放旧 visualizer
                    visualizer?.enabled = false
                    visualizer?.release()
                    visualizer = null

                    // 用 root 获取当前播放 APP 的 audio sessionId
                    val sessionId = findAudioSession()
                    sessionLabel = if (sessionId > 0) "session:$sessionId" else ""

                    if (sessionId > 0) {
                        val viz = Visualizer(sessionId)
                        val range = Visualizer.getCaptureSizeRange()
                        viz.captureSize = range[1] / 2

                        viz.setDataCaptureListener(
                            object : Visualizer.OnDataCaptureListener {
                                override fun onWaveFormDataCapture(v: Visualizer?, wf: ByteArray?, sr: Int) {}
                                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, sr: Int) {
                                    if (fft != null) {
                                        fftBytes = fft.copyOf()
                                        hasData = true
                                    }
                                }
                            },
                            Visualizer.getMaxCaptureRate() / 2,
                            false, true
                        )

                        viz.enabled = true
                        visualizer = viz

                        // 保持这个 visualizer 一小段时间
                        for (i in 0..40) {
                            if (!isActive) break
                            hue = (hue + 1f) % 360f
                            delay(50)
                        }
                    } else {
                        // 没找到 session，只做 hue 动画
                        for (i in 0..200) {
                            if (!isActive) break
                            hue = (hue + 1f) % 360f
                            delay(50)
                        }
                    }
                } catch (_: Exception) {
                    for (i in 0..100) {
                        if (!isActive) break
                        hue = (hue + 1f) % 360f
                        delay(50)
                    }
                }
            }

            visualizer?.enabled = false
            visualizer?.release()
        }
    }

    Canvas(modifier = modifier.clip(shape).background(fillColor, shape)) {
        val barCount = 48
        val barW = size.width / barCount * 0.7f
        val gap = size.width / barCount * 0.3f
        val maxH = size.height * 0.80f
        val baseH = size.height * 0.05f
        val baseY = size.height * 0.92f

        val vals = if (fftBytes.size >= barCount) {
            (0 until barCount).map { ((fftBytes[it].toInt() and 0xFF) / 255f).coerceIn(0f, 1f) }
        } else List(barCount) { 0.05f }

        (0 until barCount).forEach { i ->
            val h = (hue + i * 4f) % 360f
            val v = vals[i]
            val barH = if (v > 0.06f && hasData) (v * maxH).coerceAtLeast(baseH) else baseH
            val alpha = if (hasData && v > 0.06f) 0.9f else 0.25f

            drawRect(
                color = Color.hsl(h, 0.85f, 0.55f, alpha),
                topLeft = Offset(i * (barW + gap), baseY - barH),
                size = Size(barW, barH)
            )
        }
    }
}

/**
 * 通过 root 调用 dumpsys media.audio_flinger，找到当前播放中 APP 的 audio sessionId
 */
private fun findAudioSession(): Int {
    return try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys media.audio_flinger"))
        val out = p.inputStream.bufferedReader().use { it.readText() }
        p.waitFor()

        // 需要找到有播放活跃的 session（不含系统 uid）
        // 格式：
        //     3593    1   21945  10406  com.kugou.android
        val regex = Regex("""^\s*(\d+)\s+\d+\s+\d+\s+10\d{3}\s+(com\.\w+\.\w+)""", RegexOption.MULTILINE)
        val match = regex.find(out)
        match?.groupValues?.get(1)?.toIntOrNull() ?: -1
    } catch (_: Exception) { -1 }
}