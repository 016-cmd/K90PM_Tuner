package com.k90pm.tuner.service

import java.io.File

/**
 * K90PM 音质模块 + LSPosed 启用状态检测器。
 *
 * 启动时零后台调用。用户点击"激活"后通过 Shizuku 检测。
 */
object ModuleDetector {

    private val MODULE_PATHS = listOf(
        "/data/adb/modules/k90pm_audio_plus",
        "/data/adb/ksu/modules/k90pm_audio_plus",
        "/data/adb/ap/modules/k90pm_audio_plus"
    )
    private const val XPOSED_MARKER = "/data/local/tmp/xposed_loaded.marker"

    @Volatile var isInstalled = false; private set
    @Volatile var installedVersion = "未安装"; private set
    @Volatile var edition = "未知"; private set
    @Volatile var isLsposedEnabled = false; private set

    fun detect(): Boolean {
        return try {
            val found = MODULE_PATHS.firstOrNull { File(it, "module.prop").exists() }
            if (found != null) {
                isInstalled = true
                val props = File(found, "module.prop").readLines()
                installedVersion = props.firstOrNull { it.startsWith("version=") }?.substringAfter("=")?.trim() ?: "未知版本"
                edition = when {
                    props.any { it.contains("Ultra", true) } -> "Ultra"
                    props.any { it.contains("Standard", true) } -> "Standard"
                    else -> "未知"
                }
            } else {
                isInstalled = false; installedVersion = "未安装"; edition = "未知"
            }
            isLsposedEnabled = File(XPOSED_MARKER).exists()
            isInstalled
        } catch (_: Exception) {
            isInstalled = false; isLsposedEnabled = false; false
        }
    }
}