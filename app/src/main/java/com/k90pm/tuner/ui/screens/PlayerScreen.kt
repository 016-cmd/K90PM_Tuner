package com.k90pm.tuner.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 播放器页面路由 — 两个完全独立的子系统，互不共享状态
 *   外挂检测：仅显示外部播放器歌曲信息 + 语录
 *   在线搜索：搜索 + ExoPlayer 流式播放 + 歌词
 */
@Composable
fun PlayerScreen(activity: Activity) {
    var mode by remember { mutableStateOf(0) } // 0=外挂, 1=搜索
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 60.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 模式切换（两个按钮）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { mode = 0 }) {
                Text("外挂检测", fontWeight = if (mode == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (mode == 0) colors.primary else colors.onSurfaceVariant)
            }
            TextButton(onClick = { mode = 1 }) {
                Text("在线搜索", fontWeight = if (mode == 1) FontWeight.Bold else FontWeight.Normal,
                    color = if (mode == 1) colors.primary else colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))

        // 两个独立系统，切换时 Compose 会自动 dispose 对侧的全部状态
        when (mode) {
            0 -> ExtModeScreen(activity)
            1 -> LocalModeScreen(activity)
        }
    }
}