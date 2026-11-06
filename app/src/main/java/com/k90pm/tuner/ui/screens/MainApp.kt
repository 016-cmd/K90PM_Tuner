package com.k90pm.tuner.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.k90pm.tuner.ui.theme.Appearance
import com.k90pm.tuner.ui.theme.jellyPress
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.k90pm.tuner.music.FavoriteSong
import com.k90pm.tuner.music.MusicApi
import com.k90pm.tuner.music.MusicDatabase

/**
 * 主框架 — 内容区 + 底部液态玻璃 Dock 栏
 * 完全按照官方 Glass Bottom Bar 教程结构实现
 */
@Composable
fun MainApp(activity: Activity) {
    var currentTab by remember { mutableIntStateOf(0) }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentSong by remember { mutableStateOf<MusicApi.Song?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MusicApi.Song>>(emptyList()) }

    val ctx = LocalContext.current
    var favoriteSongs by remember { mutableStateOf<List<FavoriteSong>>(emptyList()) }

    LaunchedEffect(Unit) {
        val db = MusicDatabase.getInstance(ctx)
        favoriteSongs = db.favoriteSongDao().getAll()
        // V4A 数据层已由新的 v4a2/V4AScreen 内部通过 ViperContainer 惰性初始化
    }

    fun switchTab(index: Int) {
        if (index != currentTab) currentTab = index
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer?.release() }
    }

    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f
    val surfaceColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)

    // 液态玻璃:backdrop 不画底色,让底层内容透过来
    val backdrop = rememberLayerBackdrop {
        drawContent()
    }

    // 官方示例:最外层 Box,layerBackdrop 绑在内容组件上
    Box(modifier = Modifier.fillMaxSize()) {
        // layerBackdrop 绑在内容区（相当于官方的 MainNavHost）
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(animationSpec = tween(300)) { it * dir } + fadeIn(animationSpec = tween(300))) togetherWith
                (slideOutHorizontally(animationSpec = tween(300)) { -it * dir } + fadeOut(animationSpec = tween(300)))
            },
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) { tab ->
            when (tab) {
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
                3 -> com.k90pm.tuner.v4a2.ui.V4AScreen()
            }
        }
        // 底部液态玻璃 Dock：thumb 可拖动，拖到哪一格就切哪一页
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(62.dp)
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
            val tabs = listOf(
                DockTab("主页", Icons.Default.Home),
                DockTab("播放器", Icons.Default.MusicNote),
                DockTab("调音台", Icons.Default.Tune),
                DockTab("音效", Icons.Default.Equalizer)
            )
            val n = tabs.size
            val cellW = maxWidth / n
            var dragX by remember { mutableFloatStateOf(0f) }
            val thumbX = cellW * currentTab + dragX.dp
            val thumbColor =
                if (isDark) Color.White.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.60f)

            Box(Modifier.fillMaxSize()) {
                // 选中胶囊（thumb）
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
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(n, cellW) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragX = 0f },
                                onDragEnd = {
                                    val cw = cellW.toPx().coerceAtLeast(1f)
                                    val target = ((currentTab * cw + dragX + cw / 2f) / cw)
                                        .toInt().coerceIn(0, n - 1)
                                    dragX = 0f
                                    switchTab(target)
                                },
                                onDragCancel = { dragX = 0f },
                                onHorizontalDrag = { _, delta ->
                                    val cw = cellW.toPx()
                                    val minX = -currentTab * cw
                                    val maxX = (n - 1 - currentTab) * cw
                                    dragX = (dragX + delta).coerceIn(minX, maxX)
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = currentTab == index
                        val tabColor = if (selected) colors.primary
                        else colors.onSurfaceVariant.copy(alpha = if (isDark) 0.6f else 0.72f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .jellyPress(Appearance.jelly > 0)
                                .clickable { switchTab(index) }
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