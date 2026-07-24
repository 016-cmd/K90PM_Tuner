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
 * 频谱特效 — 有音频时动态频谱，无音频时静态底柱 + RGB 流光
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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val visualizer = Visualizer(0)
                val range = Visualizer.getCaptureSizeRange()
                visualizer.captureSize = range[1] / 2

                visualizer.setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(viz: Visualizer?, wf: ByteArray?, sr: Int) {}
                        override fun onFftDataCapture(viz: Visualizer?, fft: ByteArray?, sr: Int) {
                            if (fft != null) {
                                fftBytes = fft.copyOf()
                                hasData = true
                            }
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
            } catch (_: Exception) {
                while (isActive) {
                    hue = (hue + 1f) % 360f
                    kotlinx.coroutines.delay(50)
                }
            }
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