package com.k90pm.tuner.service

/**
 * K90PM 音质模块 + LSPosed 启用状态检测器。
 *
 * 通过 Runtime.exec("su") 检测，与 Operit 相同机制。
 * 用户在 Magisk 永久授权后 su 不弹窗。
 */
object ModuleDetector {

    @Volatile var isInstalled = false; private set
    @Volatile var installedVersion = "未安装"; private set
    @Volatile var edition = "未知"; private set
    @Volatile var isLsposedEnabled = false; private set

    fun detect() {
        val modProp = WsaShell.execSyncCmd("cat /data/adb/modules/k90pm_audio_plus/module.prop 2>/dev/null")
        if (modProp.isNotBlank()) {
            isInstalled = true
            installedVersion = modProp.lines().firstOrNull { it.startsWith("version=") }?.substringAfter("=")?.trim() ?: "未知版本"
            edition = when {
                modProp.contains("Ultra", true) -> "Ultra"
                modProp.contains("Standard", true) -> "Standard"
                else -> "未知"
            }
        } else {
            isInstalled = false; installedVersion = "未安装"; edition = "未知"
        }
    }

    /** 检测 LSPosed 是否已加载本模块 */
    fun checkLsposedEnabled() {
        isLsposedEnabled = try {
            // 方式1: 检查 ModuleHook 写入的标记文件
            if (java.io.File("/data/local/tmp/xposed_loaded.marker").exists()) {
                return@try true
            }
            // 方式2: 检查 app 内部 fallback 标记
            if (java.io.File("/data/data/com.k90pm.tuner/files/xposed_loaded.marker").exists()) {
                return@try true
            }
            // 方式3: strings 读 LSPosed 数据库
            val db = "/data/adb/lspd/config/modules_config.db"
            val out = WsaShell.execSyncCmd("strings $db")
            out.contains("com.k90pm.tuner")
        } catch (_: Exception) { false }
    }
}