package com.k90pm.tuner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── 品牌色：深灰 / 暖金 ──
private val DarkBackground = Color(0xFF0D0D0F)
private val DarkSurface = Color(0xFF1A1A1E)
private val DarkSurfaceVariant = Color(0xFF25252B)
private val WarmGold = Color(0xFFD4A853)
private val WarmGoldDim = Color(0xFF8B6F3A)
private val AccentBlue = Color(0xFF5B9BD5)
private val TextPrimary = Color(0xFFEBEBF0)
private val TextSecondary = Color(0xFF9A9AA6)
private val TextDisabled = Color(0xFF555561)

private val K90DarkColorScheme = darkColorScheme(
    primary = WarmGold,
    onPrimary = Color(0xFF1A1A1E),
    primaryContainer = WarmGoldDim,
    onPrimaryContainer = Color(0xFFFFF0D0),
    secondary = AccentBlue,
    onSecondary = Color(0xFF0D0D0F),
    secondaryContainer = Color(0xFF1B3A5C),
    onSecondaryContainer = Color(0xFFB8D8F8),
    tertiary = Color(0xFF64B5B0),
    onTertiary = Color(0xFF0D0D0F),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF3A3A45),
    outlineVariant = Color(0xFF25252B),
    error = Color(0xFFCF6679),
    onError = Color(0xFF0D0D0F),
    scrim = Color.Black
)

@Composable
fun K90TunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = K90DarkColorScheme,
        typography = Typography(),
        content = content
    )
}