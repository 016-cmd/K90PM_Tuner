package com.k90pm.tuner.ui.screens

import android.app.Activity
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.k90pm.tuner.music.MusicApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(activity: Activity) {
    val ctx = LocalContext.current
    val helper = remember { MediaSessionHelper(ctx) }

    var isLocalMode by remember { mutableStateOf(false) }
    var extSongInfo by remember { mutableStateOf(MediaSessionHelper.SongInfo()) }
    var extAvailable by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MusicApi.Song>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var currentSong by remember { mutableStateOf<MusicApi.Song?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var localPositionMs by remember { mutableStateOf(0L) }
    var localIsPlaying by remember { mutableStateOf(false) }

    var lyricLines by remember { mutableStateOf<List<LyricFetcher.LyricLine>>(emptyList()) }
    var lyricSource by remember { mutableStateOf("") }
    var lyricLoading by remember { mutableStateOf(false) }
    var lastLyricKey by remember { mutableStateOf("") }

    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val cardBg = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.40f)
    val cardShape = RoundedCornerShape(24.dp)

    LaunchedEffect(isLocalMode) {
        if (isLocalMode) return@LaunchedEffect
        while (true) {
            val info = withContext(Dispatchers.IO) { withTimeoutOrNull(2000) { helper.getSongInfo() } }
            if (info != null) { extSongInfo = info; extAvailable = info.packageName.isNotEmpty() }
            delay(2000)
        }
    }

    LaunchedEffect(exoPlayer, currentSong) {
        val player = exoPlayer ?: return@LaunchedEffect
        val song = currentSong ?: return@LaunchedEffect
        val id = song.hashCode()
        while (player == exoPlayer && currentSong?.hashCode() == id) {
            localPositionMs = maxOf(0, player.currentPosition)
            localIsPlaying = player.isPlaying
            delay(150)
        }
    }

    LaunchedEffect(currentSong, extSongInfo.title, extSongInfo.artist) {
        val title: String; val artist: String
        if (currentSong != null) { title = currentSong!!.title; artist = currentSong!!.artist }
        else if (extSongInfo.title.isNotEmpty() && extSongInfo.title != "未知歌曲") { title = extSongInfo.title; artist = extSongInfo.artist }
        else { lyricLines = emptyList(); lyricSource = ""; return@LaunchedEffect }
        val key = "$title|$artist"
        if (key == lastLyricKey) return@LaunchedEffect
        lastLyricKey = key
        lyricLoading = true
        var fetched = false
        if (currentSong != null) {
            val lrc = MusicApi.getLyric(currentSong!!.source, currentSong!!.id)
            if (!lrc.isNullOrBlank()) { lyricLines = parseLrc(lrc); lyricSource = sourceLabel(currentSong!!.source); fetched = true }
        }
        if (!fetched) {
            val r = LyricFetcher.fetch(title, artist)
            if (r != null) { lyricLines = r.lines; lyricSource = r.source } else { lyricLines = emptyList(); lyricSource = "" }
        }
        lyricLoading = false
    }

    DisposableEffect(Unit) { onDispose { exoPlayer?.release() } }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 60.dp, bottom = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { isLocalMode = false; exoPlayer?.release(); exoPlayer = null; currentSong = null; searchResults = emptyList() }) {
                Text("外挂检测", fontWeight = if (!isLocalMode) FontWeight.Bold else FontWeight.Normal, color = if (!isLocalMode) colors.primary else colors.onSurfaceVariant)
            }
            TextButton(onClick = { isLocalMode = true }) {
                Text("在线搜索", fontWeight = if (isLocalMode) FontWeight.Bold else FontWeight.Normal, color = if (isLocalMode) colors.primary else colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (isLocalMode) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("搜索歌曲...", color = colors.onSurfaceVariant.copy(0.5f)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.onSurfaceVariant.copy(0.5f)) },
                trailingIcon = {
                    if (searchLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else TextButton(onClick = {
                        if (searchQuery.isNotBlank()) { searchLoading = true; currentSong = null; exoPlayer?.release(); exoPlayer = null; CoroutineScope(Dispatchers.IO).launch { searchResults = MusicApi.search(searchQuery); searchLoading = false } }
                    }) { Text("搜索") }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary.copy(0.3f), unfocusedBorderColor = colors.onSurfaceVariant.copy(0.15f)))
            Spacer(Modifier.height(8.dp))
        }

        if (isLocalMode && searchResults.isNotEmpty() && currentSong == null) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f).clip(cardShape).background(cardBg, cardShape)) {
                items(searchResults) { song ->
                    Row(Modifier.fillMaxWidth().clickable {
                        currentSong = song; searchResults = emptyList()
                        exoPlayer?.release()
                        exoPlayer = ExoPlayer.Builder(ctx).build().also { p ->
                            p.playWhenReady = true
                            CoroutineScope(Dispatchers.IO).launch {
                                val u = MusicApi.getStreamUrl(song.source, song.id)
                                p.setMediaItem(MediaItem.fromUri(u ?: "https://mobi-api.likegamex.top/tunefree/stream?platform=${song.source}&id=${song.id}"))
                                p.prepare()
                            }
                        }
                    }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${song.artist}  ·  ${song.album}".trim().trimStart('.'), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant.copy(0.6f), maxLines = 1)
                        }
                        val sc = when(song.source) { "netease" -> Color(0xFFE72D2D); "qq" -> Color(0xFF31C27C); "kuwo" -> Color(0xFFF5A623); else -> colors.primary }
                        Surface(shape = RoundedCornerShape(4.dp), color = sc.copy(0.15f)) {
                            Text(sourceLabel(song.source), style = MaterialTheme.typography.labelSmall, color = sc, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().clip(cardShape).background(cardBg, cardShape).padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(when { isLocalMode && currentSong != null -> "本地播放 · ${sourceLabel(currentSong!!.source)}"; extAvailable -> extSongInfo.packageName; isLocalMode -> "在搜索框中输入歌名"; else -> "未检测到播放" }, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant.copy(0.6f))
                Spacer(Modifier.height(8.dp))
                val dt = if (isLocalMode && currentSong != null) currentSong!!.title else extSongInfo.title
                val da = if (isLocalMode && currentSong != null) currentSong!!.artist else extSongInfo.artist
                val dal = if (isLocalMode && currentSong != null) currentSong!!.album else extSongInfo.album
                Text(dt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(4.dp))
                Text(da, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (dal.isNotEmpty()) { Spacer(Modifier.height(2.dp)); Text(dal, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant.copy(0.6f), maxLines = 1) }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (isLocalMode && exoPlayer != null) { exoPlayer?.seekTo(0); exoPlayer?.playWhenReady = true } else helper.execAsync("input keyevent 88") }, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(28.dp), tint = colors.onSurface) }
                    IconButton(onClick = { if (isLocalMode && exoPlayer != null) exoPlayer!!.playWhenReady = !exoPlayer!!.playWhenReady else helper.execAsync("input keyevent 85") }, Modifier.size(58.dp).clip(CircleShape).background(colors.primary)) {
                        val pl = (isLocalMode && localIsPlaying) || (!isLocalMode && extSongInfo.isPlaying)
                        Icon(if (pl) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(32.dp), tint = colors.onPrimary)
                    }
                    IconButton(onClick = { if (isLocalMode && exoPlayer != null) { exoPlayer?.release(); exoPlayer = null; currentSong = null } else helper.execAsync("input keyevent 87") }, Modifier.size(48.dp)) {
                        Icon(if (isLocalMode) Icons.Rounded.Close else Icons.Rounded.SkipNext, null, Modifier.size(28.dp), tint = colors.onSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(Modifier.fillMaxWidth().weight(1f).clip(cardShape).background(cardBg, cardShape)) {
            when {
                lyricLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp, color = colors.primary) }
                lyricLines.isNotEmpty() -> LyricViewL(lines = lyricLines, source = lyricSource, positionMs = if (isLocalMode) localPositionMs else 0L, modifier = Modifier.fillMaxSize())
                isLocalMode && currentSong != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无歌词", color = colors.onSurfaceVariant.copy(0.4f)) }
                isLocalMode && searchResults.isEmpty() && searchQuery.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.MusicNote, null, Modifier.size(48.dp), tint = colors.onSurfaceVariant.copy(0.3f)); Spacer(Modifier.height(8.dp)); Text("在线搜索并播放你喜欢的歌曲", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant.copy(0.4f)) } }
                isLocalMode -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("输入歌名开始搜索", color = colors.onSurfaceVariant.copy(0.4f)) }
                else -> QuoteCard(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun sourceLabel(source: String) = when(source) { "netease" -> "网易云"; "qq" -> "QQ音乐"; "kuwo" -> "酷我"; "kugou" -> "酷狗"; else -> source }

private fun parseLrc(lrc: String): List<LyricFetcher.LyricLine> {
    val re = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
    return lrc.lines().mapNotNull { line ->
        re.find(line)?.let { LyricFetcher.LyricLine(timeMs = (it.groupValues[1].toInt()*60+it.groupValues[2].toInt())*1000L+it.groupValues[3].padEnd(3,'0').toInt(), text = it.groupValues[4].trim()) }
    }
}

@Composable
private fun LyricViewL(lines: List<LyricFetcher.LyricLine>, source: String, positionMs: Long, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val idx = lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    LaunchedEffect(idx) { if (lines.isNotEmpty()) listState.scrollToItem(idx) }
    LazyColumn(modifier.padding(horizontal = 16.dp, vertical = 12.dp), state = listState, horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text("歌词来源：$source  |  ${formatTime(positionMs)}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant.copy(0.4f), modifier = Modifier.padding(bottom = 12.dp)) }
        itemsIndexed(lines, key = { i, _ -> i }) { i, line ->
            val cur = i == idx
            Text(line.text, style = if (cur) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal, color = if (cur) colors.primary else colors.onSurface.copy(0.55f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
    }
}

private fun formatTime(ms: Long): String { val s = ms/1000; return "${s/60}:%02d".format(s%60) }

class MediaSessionHelper(private val ctx: Context) {
    data class SongInfo(val title: String = "未知歌曲", val artist: String = "未知歌手", val album: String = "", val packageName: String = "", val isPlaying: Boolean = false, val positionMs: Long = 0)
    private val m = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var ctrl: MediaController? = null
    private var reg = false
    @Volatile var livePositionMs = 0L; @Volatile var liveNano = System.nanoTime(); @Volatile var livePlaying = false; @Volatile var liveTitle = ""; @Volatile var liveArtist = ""; @Volatile var liveAlbum = ""; @Volatile var livePkg = ""
    private val cb = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(s: PlaybackState?) { s?:return; livePositionMs=s.position; liveNano=System.nanoTime(); livePlaying=s.state==PlaybackState.STATE_PLAYING }
        override fun onMetadataChanged(m: MediaMetadata?) { m?:return; liveTitle=m.getString(MediaMetadata.METADATA_KEY_TITLE)?:""; liveArtist=m.getString(MediaMetadata.METADATA_KEY_ARTIST)?:""; liveAlbum=m.getString(MediaMetadata.METADATA_KEY_ALBUM)?:"" }
    }
    private fun attach(): SongInfo? {
        try {
            val ss = try { m.getActiveSessions(null) } catch (_: SecurityException) { null }
            if (!ss.isNullOrEmpty()) {
                val c = ss.firstOrNull { it.playbackState != null } ?: ss.first()
                val p = c.packageName ?: ""
                if (ctrl?.packageName != p) { ctrl?.unregisterCallback(cb); reg = false }
                if (!reg) { c.registerCallback(cb); ctrl = c; reg = true; livePkg = p; liveTitle = c.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?:""; liveArtist = c.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?:""; liveAlbum = c.metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)?:""; livePositionMs = c.playbackState?.position?:0; livePlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING }
                return SongInfo(liveTitle.ifEmpty{"未知歌曲"}, liveArtist.ifEmpty{"未知歌手"}, liveAlbum, livePkg, livePlaying, livePositionMs)
            }
        } catch (_: Exception) {}
        return null
    }
    suspend fun getSongInfo() = withContext(Dispatchers.IO) {
        attach()?.let { return@withContext it }
        try {
            val o = Runtime.getRuntime().exec(arrayOf("su","-c","dumpsys media_session")).inputStream.bufferedReader().use { it.readText() }
            var p="";var t="";var a="";var al="";var ip=false;var pos=0L;var ins=false
            for (l in o.lines()) {
                val tr = l.trim()
                if (tr.startsWith("Sessions Stack")||tr.startsWith("Active Sessions")||tr.startsWith("Active sessions")){ins=true;continue}
                if (!ins) continue
                if (tr.startsWith("Audio playback")||tr.startsWith("Media session config")) break
                when { tr.startsWith("package=")->p=tr.substringAfter("package=").trim(); tr.startsWith("state=PlaybackState")->{ val mx=Regex("state=(\\w+)\\((\\d+)\\)").find(tr); if(mx!=null)ip=mx.groupValues[2]=="3"; val px=Regex("position=(\\d+)").find(tr); if(px!=null)pos=px.groupValues[1].toLongOrNull()?:0 }; tr.startsWith("metadata:")->{ val d=tr.substringAfter("description=").trim(); if(d.isNotEmpty()&&d!="null"){val ps=d.split(", ");t=ps.getOrNull(0)?:"";a=ps.getOrNull(1)?:"";al=ps.getOrNull(2)?:""} } }
            }
            return@withContext SongInfo(t.ifEmpty{"未知歌曲"},a.ifEmpty{"未知歌手"},al,p,ip,pos)
        } catch (_: Exception) {}
        SongInfo()
    }
    fun execAsync(cmd: String) { Thread { try { Runtime.getRuntime().exec(arrayOf("su","-c",cmd)).waitFor() } catch (_: Exception) {} }.start() }
}