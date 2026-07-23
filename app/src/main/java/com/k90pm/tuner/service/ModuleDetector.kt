package com.k90pm.tuner.service

/**
 * K90PM 音质模块 + LSPosed 启用状态检测器
 *
 * **完全不调用 su！** 仅通过 Java File API 检测 root-only 目录是否可读。
 * 用户需先自己去 Magisk/APatch 给 APP 授权 root，
 * 授权后 APP 才能读取 /data/adb/ 等受保护目录。
 */
object ModuleDetector {

    private val MODULE_PATHS = listOf(
        "/data/adb/modules/k90pm_audio_plus",
        "/data/adb/ksu/modules/k90pm_audio_plus",
        "/data/adb/ap/modules/k90pm_audio_plus"
    )

    private const val XPOSED_MARKER = "/data/local/tmp/xposed_loaded.marker"
    private const val ROOT_TEST_DIR = "/data/adb"

    @Volatile var isInstalled: Boolean = false
        private set
    @Volatile var installedVersion: String = "未安装"
        private set
    @Volatile var edition: String = "未知"
        private set
    @Volatile var isLsposedEnabled: Boolean = false
        private set

    /**
     * 纯文件方式检测 root：尝试列出 /data/adb 目录。
     * 此目录仅 root 可读——如果 list() 返回非 null 说明已有 root 授权。
     * 不调用 su，不触发 Magisk 弹窗。
     */
    fun checkRootByFileAccess(): Boolean {
        return try {
            val dir = java.io.File(ROOT_TEST_DIR)
            dir.exists() && dir.canRead()
        } catch (_: Exception) { false }
    }

    /**
     * 纯文件方式完整检测（不调 su）
     */
    fun detectByFileAccess(): Boolean {
        return try {
            // 1. 检测模块安装：检查 module.prop 文件是否存在
            val found = MODULE_PATHS.firstOrNull { path ->
                try {
                    java.io.File("$path/module.prop").exists()
                } catch (_: Exception) { false }
            }

            if (found != null) {
                isInstalled = true
                try {
                    val props = java.io.File("$found/module.prop").readText()
                    installedVersion = props.lines()
                        .firstOrNull { it.startsWith("version=") }
                        ?.substringAfter("=")?.trim() ?: "未知版本"
                    edition = when {
                        props.contains("Ultra", true) -> "Ultra"
                        props.contains("Standard", true) -> "Standard"
                        else -> "未知"
                    }
                } catch (_: Exception) {
                    installedVersion = "未知版本"
                    edition = "未知"
                }
            } else {
                isInstalled = false
                installedVersion = "未安装"
                edition = "未知"
            }

            // 2. 检测 LSPosed 标记文件
            isLsposedEnabled = try {
                java.io.File(XPOSED_MARKER).exists()
            } catch (_: Exception) { false }

            isInstalled
        } catch (e: Exception) {
            isInstalled = false
            isLsposedEnabled = false
            false
        }
    }
}