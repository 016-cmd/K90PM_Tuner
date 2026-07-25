package com.k90pm.tuner.ui.screens

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k90pm.tuner.music.DspManager

// ═══════════════════════════════════════════════════════════════
// EqualizerScreen — 基于 DynamicsProcessing 的全局调音台
// 仿照 FlowMix 方案，session 0 + 最高优先级，所有 APP 生效
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(activity: Activity) {
    val ctx = LocalContext.current
    val dsp = remember { DspManager.getInstance() }

    // ── 状态订阅 ──
    val enabled by dsp.enabled.collectAsState()
    val initDone by dsp.initDone.collectAsState()
    val bandCount by dsp.numberOfBands.collectAsState()
    val bandLevelsFlow by dsp.bandLevels.collectAsState()
    val bandFreqsFlow by dsp.bandFrequencies.collectAsState()
    val mbcOn by dsp.mbcEnabled.collectAsState()
    val limiterOn by dsp.limiterEnabled.collectAsState()
    val virtOn by dsp.virtualizerOn.collectAsState()
    val balEnabled by dsp.channelBalanceEnabled.collectAsState()
    val balance by dsp.channelBalance.collectAsState()
    val leftMuted by dsp.leftMuted.collectAsState()
    val rightMuted by dsp.rightMuted.collectAsState()

    // ── 本地状态 ──
    var bandLevels by remember { mutableStateOf(floatArrayOf()) }
    var bandFreqs by remember { mutableStateOf(floatArrayOf()) }
    var showTip by remember { mutableStateOf(true) }

    // ── 初始化 ──
    LaunchedEffect(Unit) {
        dsp.initialize(ctx)
    }

    // 同步频段数据
    LaunchedEffect(bandCount, bandLevelsFlow, bandFreqsFlow) {
        if (bandCount > 0) {
            bandLevels = bandLevelsFlow
            bandFreqs = bandFreqsFlow
        }
    }

    DisposableEffect(Unit) {
        onDispose { /* DspManager 保持全局不释放 */ }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ════════════ 标题栏 ════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DSP 调音台",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (enabled) "已激活" else "已关闭",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Switch(checked = enabled, onCheckedChange = { dsp.setEnabled(it) })
            }
        }

        if (showTip) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, top = 4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "全局调音 · 所有播放器生效",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "基于 DynamicsProcessing · APP 保持运行即可 · 切后台不影响",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = { showTip = false }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // ── 关闭或未初始化 ──
        if (!enabled || !initDone || bandCount == 0) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.GraphicEq, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    val msg = when {
                        !initDone -> "DSP 初始化中…"
                        !enabled -> "调音台已关闭\n打开上方开关即可使用"
                        else -> "暂无频段数据"
                    }
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Column
        }

        Spacer(Modifier.height(6.dp))

        // ════════════ 1. PEQ 图形均衡器 ════════════
        SectionHeader("🎚 参量均衡器", "±15 dB")
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                // 频谱可视化
                if (bandLevels.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 4.dp)) {
                        Canvas(Modifier.fillMaxSize()) {
                            val w = size.width / bandLevels.size
                            bandLevels.forEachIndexed { i, v ->
                                val norm = ((v + 15f) / 30f).coerceIn(0f, 1f)
                                drawRoundRect(
                                    color = Color(0xFF6366F1).copy(alpha = 0.3f + norm * 0.5f),
                                    topLeft = Offset(i * w + w * 0.15f, size.height - norm * size.height * 0.85f),
                                    size = Size(w * 0.7f, norm * size.height * 0.85f),
                                    cornerRadius = CornerRadius(w * 0.15f)
                                )
                            }
                        }
                    }
                }

                // 频率标签
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    bandFreqs.forEach { freq ->
                        val label = when {
                            freq >= 1000 -> "${(freq / 1000).toInt()}k"
                            else -> "${freq.toInt()}"
                        }
                        Text(
                            label, fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 竖向滑块
                Row(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bandLevels.forEachIndexed { i, level ->
                        DspBandSlider(
                            levelDB = level,
                            onLevelChange = { db -> dsp.setBandGain(i, db) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 重置
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = { dsp.resetAllBands() }) {
                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("重置所有频段")
            }
        }

        Spacer(Modifier.height(14.dp))

        // ════════════ 2. 声道平衡 ════════════
        SectionHeader("🎧 声道平衡", null)
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("声道平衡", fontWeight = FontWeight.SemiBold)
                    Switch(checked = balEnabled, onCheckedChange = { dsp.setChannelBalanceEnabled(it) })
                }
                if (balEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("L", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = if (leftMuted) Color.Red else Color.Unspecified)
                        Slider(
                            value = balance,
                            onValueChange = { dsp.setChannelBalance(it) },
                            valueRange = -1f..1f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("R", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = if (rightMuted) Color.Red else Color.Unspecified)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = leftMuted, onCheckedChange = { dsp.setLeftMuted(it) })
                            Text("左声道静音", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = rightMuted, onCheckedChange = { dsp.setRightMuted(it) })
                            Text("右声道静音", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ════════════ 3. MBC 多频段压缩 ════════════
        SectionHeader("🗜 多频段压缩 (MBC)", null)
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3 段动态压缩", fontWeight = FontWeight.SemiBold)
                    Switch(checked = mbcOn, onCheckedChange = { dsp.setMbcEnabled(it) })
                }
                if (mbcOn) {
                    Text(
                        "低频 (200Hz) · 中频 (2kHz) · 高频 (15kHz)\n自动压缩各频段动态范围，声音更饱满",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ════════════ 4. Limiter ════════════
        SectionHeader("🔊 限幅器 (Limiter)", null)
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("输出限幅保护", fontWeight = FontWeight.SemiBold)
                    Switch(checked = limiterOn, onCheckedChange = { dsp.setLimiterEnabled(it) })
                }
                if (limiterOn) {
                    Text(
                        "防止削波失真 · 阈值 -3dB · 后增益 +6dB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ════════════ 5. 虚拟环绕 ════════════
        SectionHeader("🌐 虚拟环绕", null)
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("立体声扩展", fontWeight = FontWeight.SemiBold)
                    Switch(checked = virtOn, onCheckedChange = { dsp.setVirtualizerOn(it) })
                }
                if (virtOn) {
                    Text(
                        "通过 AudioSystem 参数启用虚拟环绕效果\n让声音更有空间感",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ── 区块标题 ──
@Composable
private fun SectionHeader(title: String, subtitle: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ── 单频段竖向滑块 ──
@Composable
private fun DspBandSlider(
    levelDB: Float,
    onLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        levelDB < -3 -> Color(0xFF3B82F6)
        levelDB < 0 -> Color(0xFF60A5FA)
        levelDB < 3 -> Color(0xFF22C55E)
        levelDB < 6 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Column(
        modifier = modifier.padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // dB 值标签
        Text(
            if (levelDB >= 0) "+${"%.1f".format(levelDB)}" else "%.1f".format(levelDB),
            fontSize = 8.sp,
            color = barColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 4.dp, bottom = 4.dp)
                .pointerInput(levelDB) {
                    coroutineScope {
                        launch {
                            detectVerticalDragGestures { _, dragAmount ->
                                val h = size.height
                                if (h > 0) {
                                    val dy = -dragAmount / h
                                    val newNorm = ((levelDB + 15f) / 30f + dy).coerceIn(0f, 1f)
                                    onLevelChange(((newNorm * 30f) - 15f).coerceIn(-15f, 15f))
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width * 0.7f
                val x = (size.width - w) / 2f
                val h = size.height
                val norm = ((levelDB + 15f) / 30f).coerceIn(0f, 1f)
                val fillH = norm * h * 0.95f

                // 背景轨道
                drawRoundRect(
                    barColor.copy(alpha = 0.12f),
                    topLeft = Offset(x, 0f), size = Size(w, h),
                    cornerRadius = CornerRadius(w / 2f)
                )
                // 填充
                drawRoundRect(
                    barColor,
                    topLeft = Offset(x, h - fillH), size = Size(w, fillH),
                    cornerRadius = CornerRadius(w / 2f)
                )
                // 0dB 线
                val mid = h * 0.5f
                drawRoundRect(
                    barColor.copy(alpha = 0.35f),
                    topLeft = Offset(x, mid - 1.dp.toPx()),
                    size = Size(w, 2.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            }
        }
    }
}