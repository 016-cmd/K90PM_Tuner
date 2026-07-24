package com.k90pm.tuner.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 主框架 — 内容区 + 底部浮动 Dock 栏
 */
@Composable
fun MainApp(activity: Activity) {
    var currentTab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 内容区
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                0 -> MainScreen(activity = activity)
                1 -> PlayerScreen(activity = activity)
            }
        }

        // 底部浮动 Dock 栏 — 参考 LSPosed 悬浮底栏
        FloatingDockBar(
            currentTab = currentTab,
            onTabSelected = { currentTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 16.dp)
        )
    }
}

// ── 浮动 Dock 栏 ──

data class DockTab(
    val label: String,
    val icon: ImageVector
)

@Composable
fun FloatingDockBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red * 0.299f + colors.background.green * 0.587f + colors.background.blue * 0.114f < 0.5f

    val tabs = listOf(
        DockTab("主页", Icons.Default.Home),
        DockTab("播放器", Icons.Default.MusicNote)
    )

    // 液态玻璃 Dock 栏
    val shape = RoundedCornerShape(24.dp)
    val fillColor = if (isDark) Color.Black.copy(alpha = 0.45f)
    else Color.White.copy(alpha = 0.55f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f)
    else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillColor, shape)
            .border(0.5.dp, borderColor, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = currentTab == index
                val tabColor = if (selected) colors.primary
                               else colors.onSurfaceVariant.copy(alpha = 0.6f)

                // 紧凑自定义 tab，比 NavigationBarItem 矮很多
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (selected) Modifier.background(
                                if (isDark) Color.White.copy(alpha = 0.12f)
                                else Color.Black.copy(alpha = 0.06f),
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        )
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp),
                        tint = tabColor
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                        color = tabColor
                    )
                }
            }
        }
    }
}