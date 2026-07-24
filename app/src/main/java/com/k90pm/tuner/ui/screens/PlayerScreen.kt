package com.k90pm.tuner.ui.screens

import android.app.Activity
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * 播放器页面 — 歌曲信息卡片 + 歌词显示（在线获取） + 心情语句后备
 */
@Composable
fun PlayerScreen(activity: Activity) {
    val ctx = LocalContext.current
    val helper = remember { MediaSessionHelper(ctx) }

    var songInfo by remember { mutableStateOf(MediaSessionHelper.SongInfo()) }
    var available by remember { mutableStateOf(false) }

    // 歌词状态
    var lyricLines by remember { mutableStateOf<List<LyricFetcher.LyricLine>>(emptyList()) }
    var lyricSource by remember { mutableStateOf("") }
    var lyricLoading by remember { mutableStateOf(false) }
    var lastQueryKey by remember { mutableStateOf("") }

    // 本地进度计时器 — callback anchor + local increment
    var displayPositionMs by remember { mutableStateOf(0L) }
    var tickStartNano by remember { mutableStateOf(System.nanoTime()) }
    var tickBaseMs by remember { mutableStateOf(0L) }
    var lastCallbackPos by remember { mutableStateOf(-1L) }

    // 定时刷新歌曲信息
    LaunchedEffect(Unit) {
        while (true) {
            val info = withContext(Dispatchers.IO) {
                withTimeoutOrNull(2000) { helper.getSongInfo() }
            }
            if (info != null) {
                // callback 给的 position 变化时（拖动进度条/切歌），重置锚点
                val callbackPos = helper.livePositionMs
                if (callbackPos > 0 && callbackPos != lastCallbackPos) {
                    tickBaseMs = callbackPos
                    tickStartNano = helper.livePositionNano
                    lastCallbackPos = callbackPos
                }
                songInfo = info
                available = info.packageName.isNotEmpty()
            }
            delay(1000)
        }
    }

    // 每 200ms 自增，暂停时定格
    LaunchedEffect(Unit) {
        while (true) {
            if (songInfo.isPlaying) {
                displayPositionMs = tickBaseMs + (System.nanoTime() - tickStartNano) / 1_000_000
            } else {
                // 暂停时定格当前进度
                displayPositionMs = tickBaseMs
            }
            delay(200)
        }
    }

    // 当歌曲变化时，拉取歌词
    LaunchedEffect(songInfo.title, songInfo.artist) {
        if (songInfo.title == "未知歌曲" || songInfo.title.isEmpty()) {
            lyricLines = emptyList()
            lyricSource = ""
            return@LaunchedEffect
        }
        val key = "${songInfo.title}|${songInfo.artist}"
        if (key == lastQueryKey) return@LaunchedEffect
        lastQueryKey = key
        lyricLoading = true
        val result = LyricFetcher.fetch(songInfo.title, songInfo.artist)
        if (result != null) {
            lyricLines = result.lines
            lyricSource = result.source
        } else {
            lyricLines = emptyList()
            lyricSource = ""
        }
        lyricLoading = false
    }

    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val cardBg = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.40f)
    val cardShape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (available) songInfo.packageName else "未检测到播放",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))

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

                Text(
                    songInfo.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))

                if (songInfo.album.isNotEmpty()) {
                    Text(
                        songInfo.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(16.dp))

                // 播放控制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { helper.execAsync("input keyevent 88") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, "上一首",
                            modifier = Modifier.size(28.dp), tint = colors.onSurface)
                    }
                    IconButton(
                        onClick = { helper.execAsync("input keyevent 85") },
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    ) {
                        Icon(
                            if (songInfo.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (songInfo.isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(32.dp),
                            tint = colors.onPrimary
                        )
                    }
                    IconButton(
                        onClick = { helper.execAsync("input keyevent 87") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.SkipNext, "下一首",
                            modifier = Modifier.size(28.dp), tint = colors.onSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 歌词 / 心情语句卡片 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(cardShape)
                .background(cardBg, cardShape)
        ) {
            when {
                lyricLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                    }
                }
                // 将 positionMs 传给 LyricView
                lyricLines.isNotEmpty() -> {
                    LyricView(
                        lines = lyricLines,
                        source = lyricSource,
                        positionMs = displayPositionMs,
                        isPlaying = songInfo.isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    QuoteCard(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

// ── 歌词滚动视图（带播放位置高亮） ──
@Composable
private fun LyricView(
    lines: List<LyricFetcher.LyricLine>,
    source: String,
    positionMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    // 找到当前播放到的歌词行索引（不用 remember，每次重组都算）
    val currentIndex = if (lines.isEmpty()) 0
        else lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)

    // 自动滚动到当前行 — 用 snap 而非 animate，避免动画被反复打断导致卡住
    LaunchedEffect(currentIndex) {
        if (lines.isNotEmpty()) {
            listState.scrollToItem(currentIndex)
        }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 来源提示
        item {
            Text(
                "歌词来源：$source  |  ${formatTime(positionMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        itemsIndexed(lines, key = { i, _ -> i }) { index, line ->
            val isCurrent = index == currentIndex
            Text(
                line.text,
                style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) colors.primary else colors.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

// ── MediaSession 读取工具 ──
class MediaSessionHelper(private val ctx: Context) {
    data class SongInfo(
        val title: String = "未知歌曲",
        val artist: String = "未知歌手",
        val album: String = "",
        val packageName: String = "",
        val isPlaying: Boolean = false,
        val positionMs: Long = 0
    )

    private val manager = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null
    private var callbackRegistered = false

    // callback 驱动的实时数据
    @Volatile var livePositionMs: Long = 0
    @Volatile var livePositionNano: Long = System.nanoTime()
    @Volatile var liveIsPlaying: Boolean = false
    @Volatile var liveTitle: String = ""
    @Volatile var liveArtist: String = ""
    @Volatile var liveAlbum: String = ""
    @Volatile var livePkg: String = ""

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            state ?: return
            livePositionMs = state.position
            livePositionNano = System.nanoTime()
            liveIsPlaying = state.state == PlaybackState.STATE_PLAYING
        }
        override fun onMetadataChanged(meta: MediaMetadata?) {
            meta ?: return
            liveTitle = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            liveArtist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            liveAlbum = meta.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        }
    }

    private fun ensureControllerAttached(): SongInfo? {
        try {
            val sessions = try {
                manager.getActiveSessions(null)
            } catch (_: SecurityException) { null }

            if (!sessions.isNullOrEmpty()) {
                val ctrl = sessions.firstOrNull { it.playbackState != null } ?: sessions.first()
                val pkg = ctrl.packageName ?: ""

                // 切换 controller 时重新注册 callback
                if (activeController?.packageName != pkg) {
                    activeController?.unregisterCallback(callback)
                    callbackRegistered = false
                }

                if (!callbackRegistered) {
                    ctrl.registerCallback(callback)
                    activeController = ctrl
                    callbackRegistered = true
                    livePkg = pkg
                    // 从已注册返回初始值
                    val meta = ctrl.metadata
                    val state = ctrl.playbackState
                    liveTitle = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                    liveArtist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                    liveAlbum = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
                    livePositionMs = state?.position ?: 0
                    liveIsPlaying = state?.state == PlaybackState.STATE_PLAYING
                }

                return SongInfo(
                    title = liveTitle.ifEmpty { "未知歌曲" },
                    artist = liveArtist.ifEmpty { "未知歌手" },
                    album = liveAlbum,
                    packageName = livePkg,
                    isPlaying = liveIsPlaying,
                    positionMs = livePositionMs
                )
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun getSongInfo(): SongInfo = withContext(Dispatchers.IO) {
        // 优先用已注册 callback 的 controller
        ensureControllerAttached()?.let { return@withContext it }

        // dumpsys fallback（用于首次 / controller 不可用时）
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys media_session"))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            var pkg = ""
            var title = ""
            var artist = ""
            var album = ""
            var isPlaying = false
            var positionMs = 0L
            var inStack = false

            for (line in output.lines()) {
                val t = line.trim()
                if (t.startsWith("Sessions Stack") || t.startsWith("Active Sessions") || t.startsWith("Active sessions")) {
                    inStack = true; continue
                }
                if (!inStack) continue
                if (t.startsWith("Audio playback") || t.startsWith("Media session config")) break
                when {
                    t.startsWith("package=") -> pkg = t.substringAfter("package=").trim()
                    t.startsWith("state=PlaybackState") -> {
                        val m = Regex("""state=(\w+)\((\d+)\)""").find(t)
                        if (m != null) isPlaying = m.groupValues[2] == "3"
                        val p = Regex("""position=(\d+)""").find(t)
                        if (p != null) positionMs = p.groupValues[1].toLongOrNull() ?: 0
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
                isPlaying = isPlaying,
                positionMs = positionMs
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