package com.k90pm.tuner.ui.components

import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k90pm.tuner.ui.MainViewModel

/**
 * 顶部模块检测卡片
 * 
 * 显示 K90PM 音质模块安装状态。
 * 设计语言：LSPosed 管理器风格 — 圆角卡片 + 状态指示器 + 清晰层级
 */
@Composable
fun ModuleStatusCard(
    status: MainViewModel.ModuleStatus,
    hasRoot: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态指示灯
                val indicatorColor = when {
                    status.isChecking -> MaterialTheme.colorScheme.outline
                    status.isInstalled -> Color(0xFF4CAF50)
                    else -> Color(0xFFCF6679)
                }
                val pulseAlpha: Float by if (status.isChecking) {
                    val infinite = rememberInfiniteTransition()
                    infinite.animateFloat(0.4f, 1f, infiniteRepeatable(animationSpec = tween(800)))
                } else {
                    remember { mutableFloatStateOf(1f) }
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(indicatorColor.copy(alpha = pulseAlpha))
                )
                Spacer(Modifier.width(12.dp))

                Text(
                    text = "K90PM 音质模块",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.weight(1f))

                // 刷新按钮
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 状态信息
            if (status.isChecking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "正在检测...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 已检测完成
                InfoRow("版本", status.version)
                InfoRow("类型", status.edition)

                Spacer(Modifier.height(8.dp))

                // Root 状态
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (hasRoot) Icons.Rounded.Shield else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = if (hasRoot) Color(0xFF4CAF50) else Color(0xFFCF6679),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (hasRoot) "Root 权限 · 已获取" else "Root 权限 · 未获取",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasRoot) Color(0xFF4CAF50) else Color(0xFFCF6679)
                    )
                }

                // 未安装提示
                if (!status.isInstalled) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFCF6679).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFFCF6679),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "未检测到 K90PM 音质模块（公开版或私人版）。\n"
                                        + "寄存器调节功能已锁定，其他功能不受影响。",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCF6679).copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}