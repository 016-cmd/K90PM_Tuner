package com.k90pm.tuner.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 播放器页面 — 通过 MediaSession 读取当前播放的歌曲信息并控制播放
 */
@Composable
fun PlayerScreen(activity: Activity) {
    val ctx = LocalContext.current
    val mediaInfo = remember { MediaSessionHelper(ctx) }

    // 定时刷新
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            tick++
        }
    }

    // 每次 tick 时刷新
    val songInfo = remember(tick) { mediaInfo.getSongInfo() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 60.dp, bottom = 120.dp), // 留空间给 Dock
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 标题 ──
        Text(
            "音乐播放器",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (songInfo.packageName.isNotEmpty()) "正在监听: ${songInfo.packageName}" else "未检测到播放中的音乐",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        // ── 专辑封面 ──
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── 歌曲信息 ──
        Text(
            songInfo.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            songInfo.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            songInfo.album,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ── 播放控制按钮 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上一首
            IconButton(
                onClick = { mediaInfo.skipToPrevious() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    "上一首",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // 播放/暂停
            val isPlaying = songInfo.isPlaying
            IconButton(
                onClick = {
                    if (isPlaying) mediaInfo.pause() else mediaInfo.play()
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // 下一首
            IconButton(
                onClick = { mediaInfo.skipToNext() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    "下一首",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // ── 频谱卡片占位（下一轮实现 Visualizer）──
        SpectrumPlaceholder()
    }
}

// ── 频谱占位卡片 ──

@Composable
fun SpectrumPlaceholder() {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val shape = RoundedCornerShape(20.dp)
    val fillColor = if (isDark) Color.Black.copy(alpha = 0.20f)
    else Color.White.copy(alpha = 0.35f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f)
    else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(shape)
            .background(fillColor, shape)
            .then(
                Modifier.clip(shape).let { Modifier }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.GraphicEq,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "RGB 频谱特效",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "需要通过 Visualizer API 读取系统音频",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ── MediaSession 读取工具（通过 dumpsys + root）──

class MediaSessionHelper(private val ctx: Context) {
    data class SongInfo(
        val title: String = "未知歌曲",
        val artist: String = "未知歌手",
        val album: String = "",
        val packageName: String = "",
        val isPlaying: Boolean = false
    )

    fun getSongInfo(): SongInfo {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys media_session"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val lines = output.lines()
            var pkg = ""
            var title = ""
            var artist = ""
            var album = ""
            var isPlaying = false

            for (i in lines.indices) {
                val line = lines[i].trim()
                when {
                    line.startsWith("package=") -> pkg = line.substringAfter("package=")
                    line.contains("state=PlaybackState") -> {
                        isPlaying = line.contains("state=3")
                    }
                    line.startsWith("title=") -> title = line.substringAfter("title=")
                    line.startsWith("artist=") -> artist = line.substringAfter("artist=")
                    line.startsWith("album=") -> album = line.substringAfter("album=")
                }
            }

            return SongInfo(title, artist, album, pkg, isPlaying)
        } catch (_: Exception) {}
        return SongInfo()
    }

    // 简单控制：通过 input keyevent
    fun play() = exec("input keyevent 126")  // MEDIA_PLAY
    fun pause() = exec("input keyevent 127") // MEDIA_PAUSE
    fun skipToNext() = exec("input keyevent 87")  // MEDIA_NEXT
    fun skipToPrevious() = exec("input keyevent 88") // MEDIA_PREVIOUS

    private fun exec(cmd: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
        } catch (_: Exception) {}
    }
}