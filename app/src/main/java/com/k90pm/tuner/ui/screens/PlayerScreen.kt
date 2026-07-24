package com.k90pm.tuner.ui.screens

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
 * 播放器页面 — 通过 MediaSessionManager 读取当前播放的歌曲信息并控制播放
 * 与车机/蓝牙耳机显示歌曲信息的方式相同
 */
@Composable
fun PlayerScreen(activity: Activity) {
    val ctx = LocalContext.current
    val helper = remember { MediaSessionHelper(ctx) }

    // 异步轮询 State，不在主线程阻塞
    var songInfo by remember { mutableStateOf(MediaSessionHelper.SongInfo()) }
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }
    var available by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val info = withContext(Dispatchers.IO) {
                withTimeoutOrNull(2000) { helper.getSongInfo() }
            }
            if (info != null) {
                songInfo = info
                albumArt = info.albumArt
                available = info.packageName.isNotEmpty()
            }
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 60.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "音乐播放器",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (available) "正在监听: ${songInfo.packageName}" else "未检测到播放中的音乐\n打开酷狗/QQ音乐/网易云播放即可",
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
            if (albumArt != null) {
                Image(
                    bitmap = albumArt!!.asImageBitmap(),
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

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
            IconButton(
                onClick = { helper.execAsync("input keyevent 88") },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Rounded.SkipPrevious, "上一首", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface)
            }

            IconButton(
                onClick = { helper.execAsync("input keyevent 85") },
                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (songInfo.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (songInfo.isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = { helper.execAsync("input keyevent 87") },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Rounded.SkipNext, "下一首", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(40.dp))
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(shape)
            .background(fillColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.GraphicEq, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))
            Text("RGB 频谱特效", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("通过 Visualizer API 读取系统音频", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
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
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys media_session"))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            var pkg = ""
            var title = ""
            var artist = ""
            var album = ""
            var isPlaying = false
            var firstSession = true
            var inSession = false

            for (line in output.lines()) {
                val t = line.trim()
                if (t.startsWith("Sessions stack") || t.startsWith("Active sessions")) {
                    inSession = true; continue
                }
                if (!inSession) continue
                // 遇到第二个 session 就停止
                if (!t.startsWith(" ") && t.contains(" com.")) {
                    if (!firstSession) break
                }

                when {
                    t.startsWith("package=") -> {
                        if (firstSession) pkg = t.substringAfter("package=").trim()
                    }
                    // state=PlaybackState {state=PLAYING(3), ...} 或 state=PAUSED(2)
                    t.contains("state=PlaybackState") -> {
                        // 提取 state= 后面的状态数字，PLAYING=3
                        val stateNum = Regex("""state=\w+\((\d+)\)""").find(t)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        isPlaying = stateNum == 3
                    }
                    // metadata: size=8, description=标题, 歌手, 专辑
                    t.startsWith("metadata:") -> {
                        val desc = t.substringAfter("description=").trim()
                        if (desc.isNotEmpty() && desc != "null") {
                            val parts = desc.split(", ")
                            title = parts.getOrNull(0)?.ifEmpty { null } ?: "未知歌曲"
                            artist = parts.getOrNull(1)?.ifEmpty { null } ?: "未知歌手"
                            album = parts.getOrNull(2)?.ifEmpty { null } ?: ""
                        }
                    }
                }
                // 检测 session 结束
                if (t.startsWith("    ") && !t.startsWith("      ") && firstSession && pkg.isNotEmpty()) {
                    firstSession = false
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