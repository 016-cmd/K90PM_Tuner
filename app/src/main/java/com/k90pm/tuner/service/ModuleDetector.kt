package com.k90pm.tuner.service

import java.io.File

/**
 * K90PM 音质模块 + LSPosed 启用状态检测器。
 *
 * 启动时零调用。用户点击激活后通过 Shizuku shell 检测。
 */
object ModuleDetector {

    private val MODULE_PATHS = listOf(
        "/data/adb/modules/k90pm_audio_plus",
        "/data/adb/ksu/modules/k90pm_audio_plus",
        "/data/adb/ap/modules/k90pm_audio_plus"
    )

    @Volatile var isInstalled = false; private set
    @Volatile var installedVersion = "未安装"; private set
    @Volatile var edition = "未知"; private set
    @Volatile var isLsposedEnabled = false; private set

    /** 通过 Shizuku shell 检测模块安装状态 */
    fun detect() {
        val shell = WsaShell.execSyncCmd("test -f /data/adb/modules/k90pm_audio_plus/module.prop && echo YES")
        if (shell == "YES") {
            isInstalled = true
            val props = WsaShell.execSyncCmd("cat /data/adb/modules/k90pm_audio_plus/module.prop")
            installedVersion = props.lines().firstOrNull { it.startsWith("version=") }?.substringAfter("=")?.trim() ?: "未知版本"
            edition = when {
                props.contains("Ultra", true) -> "Ultra"
                props.contains("Standard", true) -> "Standard"
                else -> "未知"
            }
        } else {
            isInstalled = false; installedVersion = "未安装"; edition = "未知"
        }
    }

    /** 通过 Shizuku shell 查询 LSPosed 数据库 */
    fun checkLsposedEnabled() {
        isLsposedEnabled = try {
            val sql = "SELECT enabled FROM modules_state WHERE module_pkg_name='com.k90pm.tuner' AND user_id=0"
            val result = WsaShell.execSyncCmd("sqlite3 /data/adb/lspd/config/modules_config.db \"$sql\"")
            result.trim() == "1"
        } catch (_: Exception) { false }
    }
}