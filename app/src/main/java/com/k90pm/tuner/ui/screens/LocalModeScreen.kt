package com.k90pm.tuner.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.k90pm.tuner.music.MusicApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 本地搜索播放模式 — 完全独立的 Composable
 * 搜索 → 点击播放 → ExoPlayer 流式播放 → 自控歌词进度
 * 与外挂模式零耦合，切换时 Compose 自动 dispose 全部状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalModeScreen(activity: Activity) {
    val ctx = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MusicApi.Song>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var currentSong by remember { mutableStateOf<MusicApi.Song?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var positionMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    var lyricLines by remember { mutableStateOf<List<LyricFetcher.LyricLine>>(emptyList()) }
    var lyricSource by remember { mutableStateOf("") }
    var lyricLoading by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val cardBg = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.40f)
    val cardShape = RoundedCornerShape(24.dp)
    val scope = rememberCoroutineScope()

    // 进度轮询
    LaunchedEffect(exoPlayer) {
        val p = exoPlayer ?: return@LaunchedEffect
        while (p == exoPlayer) {
            positionMs = maxOf(0, p.currentPosition)
            isPlaying = p.isPlaying
            delay(150)
        }
    }

    // 歌词拉取
    LaunchedEffect(currentSong) {
        val song = currentSong
        if (song == null) { lyricLines = emptyList(); lyricSource = ""; return@LaunchedEffect }
        lyricLines = emptyList(); lyricSource = ""
        lyricLoading = true
        var fetched = false
        val lrc = MusicApi.getLyric(song.source, song.id)
        if (!lrc.isNullOrBlank()) {
            lyricLines = parseLrc(lrc)
            lyricSource = sourceLabel(song.source)
            fetched = true
        }
        if (!fetched) {
            val r = LyricFetcher.fetch(song.title, song.artist)
            if (r != null) { lyricLines = r.lines; lyricSource = r.source }
        }
        lyricLoading = false
    }

    DisposableEffect(Unit) { onDispose { exoPlayer?.release() } }

    // ── 搜索框 ──
    OutlinedTextField(
        value = searchQuery, onValueChange = { searchQuery = it },
        placeholder = { Text("搜索歌曲...", color = colors.onSurfaceVariant.copy(0.5f)) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.onSurfaceVariant.copy(0.5f)) },
        trailingIcon = {
            if (searchLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else TextButton(onClick = {
                if (searchQuery.isNotBlank()) {
                    searchLoading = true
                    exoPlayer?.release(); exoPlayer = null; currentSong = null
                    lyricLines = emptyList()
                    scope.launch(Dispatchers.IO) {
                        searchResults = MusicApi.search(searchQuery)
                        searchLoading = false
                    }
                }
            }) { Text("搜索") }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary.copy(0.3f),
            unfocusedBorderColor = colors.onSurfaceVariant.copy(0.15f)
        )
    )
    Spacer(Modifier.height(8.dp))

    // ── 搜索结果 ──
    if (searchResults.isNotEmpty() && currentSong == null) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f).clip(cardShape).background(cardBg, cardShape)) {
            items(searchResults) { song ->
                Row(Modifier.fillMaxWidth().clickable {
                    currentSong = song; searchResults = emptyList()
                    exoPlayer?.release()
                    val player = ExoPlayer.Builder(ctx).build()
                    player.playWhenReady = true
                    exoPlayer = player
                    scope.launch(Dispatchers.IO) {
                        val u = MusicApi.getStreamUrl(song.source, song.id)
                        val url = u ?: "https://mobi-api.likegamex.top/tunefree/stream?platform=${song.source}&id=${song.id}"
                        withContext(Dispatchers.Main) {
                            player.setMediaItem(MediaItem.fromUri(url))
                            player.prepare()
                        }
                    }
                }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium, color = colors.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${song.artist}  ·  ${song.album}".trim('.', ' '),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(0.6f), maxLines = 1)
                    }
                    val sc = when (song.source) {
                        "netease" -> Color(0xFFE72D2D); "qq" -> Color(0xFF31C27C)
                        "kuwo" -> Color(0xFFF5A623); else -> colors.primary
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = sc.copy(0.15f)) {
                        Text(sourceLabel(song.source), style = MaterialTheme.typography.labelSmall,
                            color = sc, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }

    // ── 信息卡片 ──
    Box(Modifier.fillMaxWidth().clip(cardShape).background(cardBg, cardShape).padding(20.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (currentSong != null) "本地播放 · ${sourceLabel(currentSong!!.source)}" else "搜索歌曲开始播放",
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant.copy(0.6f))
            Spacer(Modifier.height(8.dp))
            Text(currentSong?.title ?: "",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = colors.onSurface, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(4.dp))
            Text(currentSong?.artist ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val al = currentSong?.album ?: ""
            if (al.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(al, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant.copy(0.6f), maxLines = 1)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { exoPlayer?.seekTo(0); exoPlayer?.playWhenReady = true }, Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(28.dp), tint = colors.onSurface)
                }
                IconButton(onClick = { exoPlayer?.playWhenReady = !(exoPlayer?.playWhenReady ?: false) },
                    Modifier.size(58.dp).clip(CircleShape).background(colors.primary)) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null, Modifier.size(32.dp), tint = colors.onPrimary)
                }
                IconButton(onClick = {
                    exoPlayer?.release(); exoPlayer = null; currentSong = null; lyricLines = emptyList()
                }, Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, null, Modifier.size(28.dp), tint = colors.onSurface)
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // ── 歌词区域 ──
    Box(Modifier.fillMaxWidth().weight(1f).clip(cardShape).background(cardBg, cardShape)) {
        when {
            lyricLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp, color = colors.primary)
            }
            lyricLines.isNotEmpty() -> LyricViewLocal(
                lines = lyricLines, source = lyricSource,
                positionMs = positionMs, modifier = Modifier.fillMaxSize()
            )
            currentSong != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无歌词", color = colors.onSurfaceVariant.copy(0.4f))
            }
            searchResults.isEmpty() && searchQuery.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(48.dp), tint = colors.onSurfaceVariant.copy(0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text("在线搜索并播放你喜欢的歌曲", style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant.copy(0.4f))
                }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("输入歌名开始搜索", color = colors.onSurfaceVariant.copy(0.4f))
            }
        }
    }
}

private fun sourceLabel(source: String) = when (source) {
    "netease" -> "网易云"; "qq" -> "QQ音乐"; "kuwo" -> "酷我"; "kugou" -> "酷狗"; else -> source
}

private fun parseLrc(lrc: String): List<LyricFetcher.LyricLine> {
    val re = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
    return lrc.lines().mapNotNull { line ->
        re.find(line)?.let {
            LyricFetcher.LyricLine(
                timeMs = (it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt()) * 1000L +
                        it.groupValues[3].padEnd(3, '0').toInt(),
                text = it.groupValues[4].trim()
            )
        }
    }
}

@Composable
private fun LyricViewLocal(
    lines: List<LyricFetcher.LyricLine>, source: String,
    positionMs: Long, modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val idx = lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    LaunchedEffect(idx) { if (lines.isNotEmpty()) listState.scrollToItem(idx) }
    LazyColumn(
        modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        state = listState, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("歌词来源：$source  |  ${formatTime(positionMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(0.4f),
                modifier = Modifier.padding(bottom = 12.dp))
        }
        itemsIndexed(lines, key = { i, _ -> i }) { i, line ->
            val cur = i == idx
            Text(line.text,
                style = if (cur) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal,
                color = if (cur) colors.primary else colors.onSurface.copy(0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
    }
}

private fun formatTime(ms: Long): String { val s = ms / 1000; return "${s / 60}:%02d".format(s % 60) }