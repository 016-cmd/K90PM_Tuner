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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * 频谱特效 — Visualizer 读取系统音频输出（非麦克风），RGB 流光颜色
 */
@Composable
fun SpectrumView(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(20.dp)
    val fillColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.40f)

    var fftBytes by remember { mutableStateOf(ByteArray(0)) }
    var hue by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val visualizer = Visualizer(0)
                val capSize = Visualizer.getCaptureSizeRange()[1] / 2
                visualizer.captureSize = capSize

                visualizer.setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(viz: Visualizer?, waveform: ByteArray?, sr: Int) {}
                        override fun onFftDataCapture(viz: Visualizer?, fft: ByteArray?, sr: Int) {
                            fft?.let { fftBytes = it.copyOf() }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false, true
                )

                visualizer.enabled = true

                while (isActive) {
                    hue = (hue + 1f) % 360f
                    kotlinx.coroutines.delay(50)
                }

                visualizer.enabled = false
                visualizer.release()
            } catch (_: Exception) {}
        }
    }

    Canvas(modifier = modifier.clip(shape).background(fillColor, shape)) {
        if (fftBytes.isEmpty()) return@Canvas

        val barCount = 48
        val barWidth = size.width / barCount * 0.7f
        val gap = size.width / barCount * 0.3f
        val maxH = size.height * 0.85f

        val bars = if (fftBytes.size >= barCount) {
            (0 until barCount).map { i ->
                ((fftBytes[i].toInt() and 0xFF) / 255f).coerceIn(0f, 1f)
            }
        } else List(barCount) { 0f }

        (0 until barCount).forEach { i ->
            val h = (hue + i * 4f) % 360f
            drawRect(
                color = Color.hsl(h, 0.85f, 0.6f),
                topLeft = Offset(i * (barWidth + gap), maxH - bars[i] * maxH + (size.height - maxH) / 2),
                size = Size(barWidth, (bars[i] * maxH).coerceAtLeast(2f))
            )
        }
    }
}