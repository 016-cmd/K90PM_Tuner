package com.k90pm.tuner.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * 果冻按压：按下时横向放宽、纵向压扁，松手弹回。
 * 只给按钮 / 可点行用，卡片本体不要套。
 */
@Composable
fun Modifier.jellyPress(enabled: Boolean): Modifier {
    if (!enabled) return this
    val scope = rememberCoroutineScope()
    val sx = remember { Animatable(1f) }
    val sy = remember { Animatable(1f) }
    val amt = (Appearance.jelly / 100f).coerceIn(0f, 1f)
    if (amt <= 0f) return this
    val pressX = 1f + 0.05f * amt
    val pressY = 1f - 0.07f * amt
    val spec = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = 420f,
        visibilityThreshold = 0.001f
    )
    return this
        .graphicsLayer {
            scaleX = sx.value
            scaleY = sy.value
        }
        .pointerInput(enabled, amt) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                scope.launch { sx.animateTo(pressX, spec) }
                scope.launch { sy.animateTo(pressY, spec) }
                waitForUpOrCancellation()
                scope.launch { sx.animateTo(1f, spec) }
                scope.launch { sy.animateTo(1f, spec) }
            }
        }
}