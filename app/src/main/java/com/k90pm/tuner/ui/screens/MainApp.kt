package com.k90pm.tuner.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.k90pm.tuner.music.FavoriteSong
import com.k90pm.tuner.music.MusicApi
import com.k90pm.tuner.music.MusicDatabase
import com.k90pm.tuner.ui.theme.Appearance
import com.k90pm.tuner.ui.theme.jellyPress
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 主框架 — 内容区（HorizontalPager）+ 底部液态玻璃 Dock。
 * Dock 的 thumb 跟手：拖动直接驱动 pager，松手吸附到最近一格。
 */
@Composable
fun MainApp(activity: Activity) {
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentSong by remember { mutableStateOf<MusicApi.Song?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MusicApi.Song>>(emptyList()) }
    val ctx = LocalContext.current
    var favoriteSongs by remember { mutableStateOf<List<FavoriteSong>>(emptyList()) }

    LaunchedEffect(Unit) {
        val db = MusicDatabase.getInstance(ctx)
        favoriteSongs = db.favoriteSongDao().getAll()
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer?.release() }
    }

    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f +
            colors.background.blue * 0.114f < 0.5f

    val tabs = listOf(
        DockTab("主页", Icons.Default.Home),
        DockTab("播放器", Icons.Default.MusicNote),
        DockTab("调音台", Icons.Default.Tune),
        DockTab("音效", Icons.Default.Equalizer)
    )

    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val screenW = LocalConfiguration.current.screenWidthDp.dp

    val backdrop = rememberLayerBackdrop { drawContent() }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
            beyondViewportPageCount = 1
        ) { page ->
            val off = (pager.currentPage - page) + pager.currentPageOffsetFraction
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val d = abs(off).coerceIn(0f, 1f)
                    alpha = 1f - d * 0.35f
                    val s = 1f - d * 0.05f
                    scaleX = s
                    scaleY = s
                }
            ) {
                when (page) {
                    0 -> MainScreen(activity = activity)
                    1 -> PlayerScreen(
                        activity = activity,
                        exoPlayer = exoPlayer, onExoPlayerChange = { exoPlayer = it },
                        currentSong = currentSong, onCurrentSongChange = { currentSong = it },
                        searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it },
                        searchResults = searchResults, onSearchResultsChange = { searchResults = it },
                        favoriteSongs = favoriteSongs, onFavoriteSongsChange = { favoriteSongs = it }
                    )
                    2 -> DolbyTunerScreen(activity = activity)
                    else -> com.k90pm.tuner.v4a2.ui.V4AScreen()
                }
            }
        }

        // 底部液态玻璃 Dock
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(Appearance.radius.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(Appearance.radius.dp) },
                    effects = {
                        vibrancy()
                        blur((6 + Appearance.glass * 2).dp.toPx())
                        lens(
                            (8 + Appearance.glass * 2).dp.toPx(),
                            (16 + Appearance.glass * 3).dp.toPx()
                        )
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isDark) Color.Black.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.12f)
                        )
                    }
                )
        ) {
            val cellW = maxWidth / tabs.size
            val pagePos = pager.currentPage + pager.currentPageOffsetFraction
            val thumbX = cellW * pagePos
            val thumbColor =
                if (isDark) Color.White.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.58f)

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(cellW, screenW) {
                        detectDragGestures(
                            onDrag = { change, amount ->
                                change.consume()
                                val ratio = screenW.value / cellW.value.coerceAtLeast(1f)
                                scope.launch { pager.dispatchRawDelta(amount.x * ratio) }
                            },
                            onDragEnd = {
                                val target = pager.currentPage +
                                        if (pager.currentPageOffsetFraction > 0.12f) 1
                                        else if (pager.currentPageOffsetFraction < -0.12f) -1
                                        else 0
                                scope.launch {
                                    pager.animateScrollToPage(target.coerceIn(0, tabs.size - 1))
                                }
                            }
                        )
                    }
            ) {
                // thumb：跟手移动，宽度正好一格
                Box(
                    Modifier
                        .offset { IntOffset(thumbX.roundToPx(), 0) }
                        .width(cellW)
                        .fillMaxHeight()
                        .padding(5.dp)
                        .clip(RoundedCornerShape((Appearance.radius - 6f).coerceAtLeast(10f).dp))
                        .background(thumbColor)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = pager.currentPage == index
                        val tabColor =
                            if (selected) colors.primary
                            else colors.onSurfaceVariant.copy(alpha = if (isDark) 0.62f else 0.74f)
                        val interaction = remember { MutableInteractionSource() }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .jellyPress(Appearance.jelly > 0)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) {
                                    scope.launch { pager.animateScrollToPage(index) }
                                }
                        ) {
                            Icon(
                                tab.icon, contentDescription = tab.label,
                                modifier = Modifier.size(21.dp), tint = tabColor
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = tabColor
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DockTab(val label: String, val icon: ImageVector)