package com.k90pm.tuner.ui.components

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
 * 显示 K90PM 音质模块安装状态 + LSPosed 启用状态 + Root 状态。
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
                val indicatorColor = when {
                    status.isChecking -> MaterialTheme.colorScheme.outline
                    status.isInstalled && status.isLsposedEnabled && hasRoot -> Color(0xFF4CAF50)
                    else -> Color(0xFFCF6679)
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Spacer(Modifier.width(12.dp))

                Text(
                    text = "K90PM 音质模块",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.weight(1f))

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
                InfoRow("版本", status.version)
                InfoRow("类型", status.edition)

                Spacer(Modifier.height(6.dp))

                // ── 三重状态指示 ──
                TripleStatusRow(
                    label = "LSPosed",
                    ok = status.isLsposedEnabled,
                    okText = "已启用",
                    failText = "未启用"
                )
                TripleStatusRow(
                    label = "Root",
                    ok = hasRoot,
                    okText = "已获取",
                    failText = "未获取"
                )

                // ── 状态提示 ──
                val hintText = when {
                    !status.isInstalled -> "未检测到 K90PM 音质模块\n寄存器调节功能已锁定"
                    !status.isLsposedEnabled -> "请在 LSPosed 管理器中启用本模块\n否则无法控制 WSA 寄存器"
                    !hasRoot -> "需要 Root 权限才能操作 WSA 寄存器"
                    else -> null
                }

                if (hintText != null) {
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
                                hintText,
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

/**
 * 三重状态行：OK 绿色对勾 / 失败 红色叉
 */
@Composable
private fun TripleStatusRow(
    label: String,
    ok: Boolean,
    okText: String,
    failText: String
) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            contentDescription = null,
            tint = if (ok) Color(0xFF4CAF50) else Color(0xFFCF6679),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (ok) okText else failText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (ok) Color(0xFF4CAF50) else Color(0xFFCF6679)
        )
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