package com.k90pm.tuner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k90pm.tuner.ui.theme.Appearance
import com.k90pm.tuner.ui.theme.WallpaperState
import com.k90pm.tuner.ui.theme.jellyPress
import com.k90pm.tuner.ui.theme.subInk

/**
 * 设置页里的「外观与个性化」——与 DolbyTuner 同款：展开/闭合，不跳页面。
 * 参数全部用分段按钮与开关，没有滑条。
 */
@Composable
fun AppearanceSection(
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    hasLocalWallpaper: Boolean,
    wallpaperKind: Int,
    onWallpaperKind: (Int) -> Unit
) {
    val ctx = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    val accent = Appearance.accent

    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .jellyPress(Appearance.jelly > 0)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Palette, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("外观与个性化", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(summary(), fontSize = 10.sp, color = subInk())
            }
            Icon(
                Icons.Default.KeyboardArrowDown, null,
                tint = subInk(),
                modifier = Modifier.size(20.dp).rotate(arrow)
            )
        }

        AnimatedVisibility(expanded) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(10.dp))

                // ── 玻璃 ──
                GroupLabel("玻璃")
                SegRow(
                    "玻璃风格",
                    listOf("清透", "标准", "浓厚", "柔和", "省电"),
                    Appearance.glass.coerceIn(0, 4)
                ) { Appearance.setGlass(ctx, it) }

                // ── 颜色 ──
                GroupLabel("颜色")
                SegRow(
                    "卡片底色",
                    listOf("自动", "纯白", "纯黑"),
                    Appearance.tintMode.coerceIn(0, 2)
                ) { Appearance.setTintMode(ctx, it) }

                // ── 形状 ──
                GroupLabel("形状")
                SegRow(
                    "圆角",
                    listOf("小", "标准", "大"),
                    nearest(Appearance.radius.toInt(), intArrayOf(16, 26, 32))
                ) { Appearance.setRadius(ctx, intArrayOf(16, 26, 32)[it].toFloat()) }

                // ── 动效 ──
                GroupLabel("动效")
                SegRow(
                    "果冻按压",
                    listOf("关", "轻", "标准", "强"),
                    nearest(Appearance.jelly, intArrayOf(0, 40, 70, 100))
                ) { Appearance.setJelly(ctx, intArrayOf(0, 40, 70, 100)[it]) }
                ToggleRow("页面切换动画", null, Appearance.animPage == 1) {
                    Appearance.setAnimPage(ctx, it)
                }
                ToggleRow("卡片入场动画", null, Appearance.animCard == 1) {
                    Appearance.setAnimCard(ctx, it)
                }

                // ── 壁纸 ──
                GroupLabel("壁纸")
                val wp = WallpaperState.original
                if (wp != null) {
                    Image(
                        bitmap = wp.asImageBitmap(),
                        contentDescription = "当前壁纸",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    "当前：${if (hasLocalWallpaper) "本地图片" else Appearance.builtinName(wallpaperKind)}",
                    fontSize = 9.sp,
                    color = subInk()
                )
                SegRow(
                    "来源",
                    listOf("本地", "绝区零", "海风", "微醺"),
                    wallpaperKind
                ) { onWallpaperKind(it) }
                SegRow(
                    "压暗",
                    listOf("无", "轻", "中", "深"),
                    nearest(Appearance.wallpaperDim, intArrayOf(0, 20, 40, 60))
                ) { Appearance.setWallpaperDim(ctx, intArrayOf(0, 20, 40, 60)[it]) }
                Spacer(Modifier.height(4.dp))
                if (hasLocalWallpaper) {
                    WallButton("清除本地壁纸", Icons.Default.Delete) { onClearWallpaper() }
                    Spacer(Modifier.height(8.dp))
                }
                WallButton("从本地选择图片", Icons.Default.PhotoLibrary) { onPickWallpaper() }

                // ── 文字 ──
                GroupLabel("文字")
                ToggleRow(
                    "增强文字可读性",
                    "透明卡片上看不清时打开：小字改成纯黑/纯白",
                    Appearance.readLevel >= 1
                ) { Appearance.setReadLevel(ctx, if (it) 1 else 0) }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private fun wallpaperKind: Int =
    if (WallpaperState.original == null) 0 else 0

private fun nearest(v: Int, arr: IntArray): Int {
    var best = 0
    var bd = Int.MAX_VALUE
    arr.forEachIndexed { i, x ->
        val d = kotlin.math.abs(x - v)
        if (d < bd) { bd = d; best = i }
    }
    return best
}

private fun summary(): String {
    val glass = listOf("清透", "标准", "浓厚", "柔和", "省电")[Appearance.glass.coerceIn(0, 4)]
    val tint = when (Appearance.tintMode) { 1 -> "纯白"; 2 -> "纯黑"; else -> "自动" }
    return "液态玻璃 · $glass · $tint · 圆角 ${Appearance.radius.toInt()}dp"
}

@Composable
private fun GroupLabel(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = subInk(0.62f)
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 9.sp, color = subInk())
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Appearance.accent),
            modifier = Modifier.size(width = 40.dp, height = 24.dp)
        )
    }
}

@Composable
private fun SegRow(
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Text(title, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    Row(
        Modifier.fillMaxWidth().padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEachIndexed { i, s ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (on) Appearance.accent.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                    .jellyPress(Appearance.jelly > 0)
                    .clickable { onSelect(i) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s,
                    fontSize = 10.sp,
                    color = if (on) Appearance.accent else subInk(0.72f),
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** 整宽白底按钮：用来放「从本地选择图片 / 清除本地壁纸」 */
@Composable
private fun WallButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val ink = Color(0xFF1A1A1E)
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .jellyPress(Appearance.jelly > 0)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = ink, fontWeight = FontWeight.Medium)
    }
}