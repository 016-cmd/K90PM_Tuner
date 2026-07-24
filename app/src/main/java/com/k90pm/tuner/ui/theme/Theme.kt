package com.k90pm.tuner.ui.theme

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import java.io.File

// ── 品牌色 ──
private val WarmGold = Color(0xFFD4A853)
private val WarmGoldDim = Color(0xFF8B6F3A)
private val AccentBlue = Color(0xFF5B9BD5)

private val LightBackground = Color(0xFFF5F5F7)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFEBEBF0)
private val LightTextPrimary = Color(0xFF1A1A1E)
private val LightTextSecondary = Color(0xFF6B6B78)

private val DarkBackground = Color(0xFF0D0D0F)
private val DarkSurface = Color(0xFF1A1A1E)
private val DarkSurfaceVariant = Color(0xFF25252B)
private val DarkTextPrimary = Color(0xFFEBEBF0)
private val DarkTextSecondary = Color(0xFF9A9AA6)

private val LightColorScheme = lightColorScheme(
    primary = WarmGold,
    onPrimary = Color.White,
    primaryContainer = WarmGoldDim.copy(alpha = 0.3f),
    onPrimaryContainer = WarmGoldDim,
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = AccentBlue.copy(alpha = 0.15f),
    onSecondaryContainer = AccentBlue,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFC8C8D0),
    outlineVariant = Color(0xFFE0E0E6),
    error = Color(0xFFCF6679),
    onError = Color.White,
    scrim = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = WarmGold,
    onPrimary = Color(0xFF1A1A1E),
    primaryContainer = WarmGoldDim,
    onPrimaryContainer = Color(0xFFFFF0D0),
    secondary = AccentBlue,
    onSecondary = Color(0xFF0D0D0F),
    secondaryContainer = Color(0xFF1B3A5C),
    onSecondaryContainer = Color(0xFFB8D8F8),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF3A3A45),
    outlineVariant = Color(0xFF25252B),
    error = Color(0xFFCF6679),
    onError = Color(0xFF0D0D0F),
    scrim = Color.Black
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private const val PREFS_THEME = "k90pm_tuner"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_WALLPAPER = "wallpaper_uri"

object ThemePrefs {
    fun getMode(ctx: Context): ThemeMode = try {
        ThemeMode.valueOf(ctx.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE).getString(KEY_THEME_MODE, "SYSTEM")!!)
    } catch (_: Exception) { ThemeMode.SYSTEM }

    fun setMode(ctx: Context, mode: ThemeMode) {
        ctx.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getWallpaperUri(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE).getString(KEY_WALLPAPER, null)

    fun setWallpaperUri(ctx: Context, uri: String?) {
        ctx.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE).edit().putString(KEY_WALLPAPER, uri).apply()
    }
}

// ── K90TunerTheme ──
@Composable
fun K90TunerTheme(content: @Composable () -> Unit) {
    // recreate() 后整个 Composable 树重建，直接从 SharedPreferences 读取
    val ctx = LocalContext.current
    val mode = ThemePrefs.getMode(ctx)
    val wallpaperUri = ThemePrefs.getWallpaperUri(ctx)

    val isDark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    // 壁纸变化时异步加载 bitmap → 模糊 → WallpaperState
    LaunchedEffect(wallpaperUri, isDark) {
        if (wallpaperUri != null) {
            try {
                withContext(Dispatchers.IO) {
                    val bitmap = if (wallpaperUri.startsWith("content://")) {
                        ctx.contentResolver.openInputStream(
                            android.net.Uri.parse(wallpaperUri)
                        )?.use { BitmapFactory.decodeStream(it) }
                    } else {
                        BitmapFactory.decodeFile(wallpaperUri)
                    }
                    if (bitmap != null) {
                        WallpaperState.set(bitmap, isDark)
                    }
                }
            } catch (_: Exception) {}
        } else {
            WallpaperState.clear()
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography()) {
        Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
            if (wallpaperUri != null) {
                val model: Any = if (wallpaperUri.startsWith("content://")) wallpaperUri else File(wallpaperUri)
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // 内容层直接叠加在壁纸（或纯色背景）上
            content()
        }
    }
}