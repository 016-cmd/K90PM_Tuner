package com.k90pm.tuner.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 音乐在线搜索 + 流式播放 API
 * 后端：mobi-api.likegamex.top (TuneFree 聚合 API)
 * 备选：网易云官方 API
 */
object MusicApi {

    data class Song(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val coverUrl: String,
        val durationMs: Long,
        val source: String  // "netease" / "qq" / "kuwo" / "kugou"
    )

    /**
     * 多平台搜索歌曲
     * @param keyword 搜索关键词
     * @param sources 要搜索的平台列表，默认三平台
     */
    suspend fun search(
        keyword: String,
        sources: List<String> = listOf("netease", "qq", "kuwo")
    ): List<Song> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Song>()
        for (src in sources) {
            try {
                results.addAll(searchPlatform(keyword, src))
            } catch (_: Exception) {}
        }
        // 去重：同歌名+歌手只保留一个
        results.distinctBy { "${it.title}|${it.artist}".lowercase() }
    }

    private fun searchPlatform(keyword: String, platform: String): List<Song> {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        return when (platform) {
            "netease" -> searchNetease(encoded)
            "qq" -> searchQQ(encoded)
            "kuwo" -> searchKuwo(encoded)
            else -> emptyList()
        }
    }

    // ── 网易云搜索 ──
    private fun searchNetease(keyword: String): List<Song> {
        val url = "https://music.163.com/api/search/get/web?s=$keyword&type=1&offset=0&limit=20"
        val json = httpGet(url, referer = "https://music.163.com/") ?: return emptyList()
        val songs = JSONObject(json).optJSONObject("result")?.optJSONArray("songs")
            ?: return emptyList()
        val list = mutableListOf<Song>()
        for (i in 0 until songs.length()) {
            val s = songs.getJSONObject(i)
            val artists = s.optJSONArray("artists")
            val artist = artists?.let { a ->
                (0 until a.length()).joinToString(", ") { a.getJSONObject(it).optString("name", "") }
            } ?: ""
            val album = s.optJSONObject("album")?.optString("name", "") ?: ""
            list.add(Song(
                id = s.optString("id"),
                title = s.optString("name"),
                artist = artist,
                album = album,
                coverUrl = albumUrl(s.optJSONObject("album")),
                durationMs = s.optLong("duration"),
                source = "netease"
            ))
        }
        return list
    }

    private fun albumUrl(album: JSONObject?): String {
        val picId = album?.optLong("picId") ?: 0
        if (picId > 0) return "https://p2.music.126.net/xxx.jpg" // placeholder
        return album?.optString("picUrl", "") ?: ""
    }

    // ── QQ 音乐搜索 ──
    private fun searchQQ(keyword: String): List<Song> {
        val url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=$keyword&format=json&n=20"
        val json = httpGet(url, referer = "https://y.qq.com") ?: return emptyList()
        val songList = JSONObject(json).optJSONObject("data")?.optJSONObject("song")
            ?.optJSONArray("list") ?: return emptyList()
        val list = mutableListOf<Song>()
        for (i in 0 until songList.length()) {
            val s = songList.getJSONObject(i)
            val singers = s.optJSONArray("singer")
            val artist = singers?.let { a ->
                (0 until a.length()).joinToString(", ") { a.getJSONObject(it).optString("name", "") }
            } ?: ""
            val songmid = s.optString("songmid")
            val albummid = s.optString("albummid")
            list.add(Song(
                id = songmid,
                title = s.optString("songname"),
                artist = artist,
                album = s.optString("albumname"),
                coverUrl = "https://y.gtimg.cn/music/photo_new/T002R300x300M000${albummid}.jpg",
                durationMs = s.optLong("interval") * 1000,
                source = "qq"
            ))
        }
        return list
    }

    // ── 酷我搜索 ──
    private fun searchKuwo(keyword: String): List<Song> {
        val url = "https://search.kuwo.cn/r.s?all={$keyword}&ft=music&pn=0&rn=20&rformat=json&encoding=utf8"
            .replace("{$keyword}", keyword) // kuwo uses different encoding
        val json = httpGet(url) ?: return emptyList()
        val absList = JSONObject(json).optJSONArray("abslist") ?: return emptyList()
        val list = mutableListOf<Song>()
        for (i in 0 until absList.length()) {
            val s = absList.getJSONObject(i)
            list.add(Song(
                id = s.optString("MUSICRID", "").replace("MUSIC_", ""),
                title = s.optString("NAME"),
                artist = s.optString("ARTIST"),
                album = s.optString("ALBUM"),
                coverUrl = "",
                durationMs = s.optLong("DURATION") * 1000,
                source = "kuwo"
            ))
        }
        return list
    }

    // ── 获取流式播放 URL（通过 mobi-api）──
    suspend fun getStreamUrl(platform: String, songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://mobi-api.likegamex.top/tunefree/stream?platform=$platform&id=$songId"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connect()
            // stream 端点返回 302 或直接 mp3 流，取 Location
            if (conn.responseCode == 302 || conn.responseCode == 307) {
                conn.getHeaderField("Location")
            } else {
                // 如果直接返回流，返回原始 URL 供 ExoPlayer 用
                url
            }
        } catch (_: Exception) { null }
    }

    // ── 获取歌词（复用 LyricFetcher 的逻辑，但接受 songId）──
    suspend fun getLyric(platform: String, songId: String): String? = withContext(Dispatchers.IO) {
        when (platform) {
            "netease" -> {
                val json = httpGet(
                    "https://music.163.com/api/song/lyric?id=$songId&lv=1",
                    referer = "https://music.163.com/"
                ) ?: return@withContext null
                JSONObject(json).optJSONObject("lrc")?.optString("lyric")
            }
            "qq" -> {
                val json = httpGet(
                    "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=$songId&format=json&nobase64=1",
                    referer = "https://y.qq.com"
                ) ?: return@withContext null
                val base64 = JSONObject(json).optString("lyric") ?: return@withContext null
                String(android.util.Base64.decode(base64, android.util.Base64.DEFAULT), Charsets.UTF_8)
            }
            else -> null
        }
    }

    // ── HTTP GET ──
    private fun httpGet(urlStr: String, referer: String? = null): String? {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            if (referer != null) conn.setRequestProperty("Referer", referer)
            conn.connect()
            if (conn.responseCode != 200) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) { null }
    }
}
