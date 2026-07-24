package com.k90pm.tuner.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 播放器页面
 */
@Composable
fun PlayerScreen(activity: Activity) {
    val ctx = LocalContext.current
    val helper = remember { MediaSessionHelper(ctx) }

    var songInfo by remember { mutableStateOf(MediaSessionHelper.SongInfo()) }
    var available by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val info = withContext(Dispatchers.IO) {
                withTimeoutOrNull(2000) { helper.getSongInfo() }
            }
            if (info != null) {
                songInfo = info
                available = info.packageName.isNotEmpty()
            }
            delay(1000)
        }
    }

    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val cardBg = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.40f)
    val cardShape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 60.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 歌曲信息 + 控制卡片 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(cardBg, cardShape)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 包名提示
                Text(
                    if (available) songInfo.packageName else "未检测到播放",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))

                // 歌曲标题
                Text(
                    songInfo.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(4.dp))

                // 歌手
                Text(
                    songInfo.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))

                // 专辑
                if (songInfo.album.isNotEmpty()) {
                    Text(
                        songInfo.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(20.dp))
                } else {
                    Spacer(Modifier.height(20.dp))
                }

                // ── 播放控制按钮 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { helper.execAsync("input keyevent 88") },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, "上一首",
                            modifier = Modifier.size(32.dp),
                            tint = colors.onSurface)
                    }

                    IconButton(
                        onClick = { helper.execAsync("input keyevent 85") },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    ) {
                        Icon(
                            if (songInfo.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (songInfo.isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(36.dp),
                            tint = colors.onPrimary
                        )
                    }

                    IconButton(
                        onClick = { helper.execAsync("input keyevent 87") },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Rounded.SkipNext, "下一首",
                            modifier = Modifier.size(32.dp),
                            tint = colors.onSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 频谱特效 ──
        SpectrumView(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    }
}

// ── MediaSession 读取工具 ──
// 同时尝试 MediaSessionManager API 和 dumpsys fallback

class MediaSessionHelper(private val ctx: Context) {
    data class SongInfo(
        val title: String = "未知歌曲",
        val artist: String = "未知歌手",
        val album: String = "",
        val packageName: String = "",
        val isPlaying: Boolean = false,
        val albumArt: Bitmap? = null
    )

    private val manager = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null
    private var registered = false

    /**
     * 获取歌曲信息。先尝试 MediaSessionManager.getActiveSessions（需要通知权限或 root），
     * 失败则 fallback 到 dumpsys（root），再失败返回空。
     */
    suspend fun getSongInfo(): SongInfo = withContext(Dispatchers.IO) {
        try {
            // 方式1：MediaSessionManager（标准 API）
            val sessions = try {
                manager.getActiveSessions(null)
            } catch (_: SecurityException) { null }

            if (!sessions.isNullOrEmpty()) {
                val ctrl = sessions.firstOrNull { it.playbackState != null } ?: sessions.first()
                activeController = ctrl
                val meta = ctrl.metadata
                val state = ctrl.playbackState
                return@withContext SongInfo(
                    title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "未知歌曲",
                    artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "未知歌手",
                    album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
                    packageName = ctrl.packageName ?: "",
                    isPlaying = state?.state == PlaybackState.STATE_PLAYING,
                    albumArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                )
            }

            // 方式2：dumpsys fallback（root）
            // 直接读 dumpsys 输出，格式稳定可靠
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys media_session"))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            var pkg = ""
            var title = ""
            var artist = ""
            var album = ""
            var isPlaying = false
            var inStack = false

            for (line in output.lines()) {
                val t = line.trim()
                // 进入 Sessions Stack
                if (t.startsWith("Sessions Stack") || t.startsWith("Active Sessions") || t.startsWith("Active sessions")) {
                    inStack = true
                    continue
                }
                if (!inStack) continue
                // 退出 Sessions Stack: 遇到 Audio playback 或其他顶级行
                if (t.startsWith("Audio playback") || t.startsWith("Media session config")) break

                when {
                    t.startsWith("package=") -> pkg = t.substringAfter("package=").trim()
                    t.startsWith("state=PlaybackState") -> {
                        val m = Regex("""state=(\w+)\((\d+)\)""").find(t)
                        if (m != null) isPlaying = m.groupValues[2] == "3"
                    }
                    t.startsWith("metadata:") -> {
                        val desc = t.substringAfter("description=").trim()
                        if (desc.isNotEmpty() && desc != "null") {
                            val parts = desc.split(", ")
                            title = parts.getOrNull(0) ?: ""
                            artist = parts.getOrNull(1) ?: ""
                            album = parts.getOrNull(2) ?: ""
                        }
                    }
                }
            }

            return@withContext SongInfo(
                title = title.ifEmpty { "未知歌曲" },
                artist = artist.ifEmpty { "未知歌手" },
                album = album,
                packageName = pkg,
                isPlaying = isPlaying
            )
        } catch (_: Exception) {}

        SongInfo()
    }

    fun execAsync(cmd: String) {
        Thread {
            try { Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor() }
            catch (_: Exception) {}
        }.start()
    }
}