package com.k90pm.tuner.service

/**
 * K90PM 音质模块检测器。
 *
 * 通过 Root 检测 Magisk 模块安装状态。
 */
object ModuleDetector {

    @Volatile var isInstalled = false; private set
    @Volatile var installedVersion = "未安装"; private set
    @Volatile var edition = "未知"; private set

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
}