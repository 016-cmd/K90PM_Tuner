package com.k90pm.tuner.service

import com.topjohnwu.superuser.Shell

/**
 * K90PM 音质模块 + LSPosed 启用状态检测器
 *
 * 使用 root shell 检测，因为 /data/adb 目录仅 root 可读。
 */
object ModuleDetector {

    private val MODULE_PATHS = listOf(
        "/data/adb/modules/k90pm_audio_plus",
        "/data/adb/ksu/modules/k90pm_audio_plus",
        "/data/adb/ap/modules/k90pm_audio_plus"
    )

    /** 标记文件路径：ModuleHook.initZygote 写入 */
    private const val XPOSED_MARKER = "/data/local/tmp/xposed_loaded.marker"

    /** 是否安装了 K90PM 音质模块 */
    @Volatile var isInstalled: Boolean = false
        private set

    @Volatile var installedVersion: String = "未安装"
        private set

    @Volatile var edition: String = "未知"
        private set

    /** LSPosed 是否已启用本模块 */
    @Volatile var isLsposedEnabled: Boolean = false
        private set

    /**
     * 执行检测（需要 root）
     * 检测项目：1) 音质模块是否存在  2) LSPosed 是否加载了 Hook
     */
    fun detect(): Boolean {
        return try {
            // 1. 检测模块安装
            val result = Shell.cmd(
                MODULE_PATHS.joinToString("; ") { "test -f $it/module.prop && echo FOUND:$it" }
            ).exec()

            var moduleFound = false
            for (line in result.out) {
                if (line.startsWith("FOUND:")) {
                    val foundPath = line.removePrefix("FOUND:")
                    isInstalled = true
                    moduleFound = true

                    val propResult = Shell.cmd("cat $foundPath/module.prop").exec()
                    val props = propResult.out.joinToString("\n")
                    installedVersion = props.lines()
                        .firstOrNull { it.startsWith("version=") }
                        ?.substringAfter("=")?.trim() ?: "未知版本"

                    edition = when {
                        props.contains("Ultra", ignoreCase = true) -> "Ultra"
                        props.contains("Standard", ignoreCase = true) -> "Standard"
                        else -> "未知"
                    }
                    break
                }
            }
            if (!moduleFound) {
                isInstalled = false
                installedVersion = "未安装"
                edition = "未知"
            }

            // 2. 检测 LSPosed 是否已加载 Hook
            isLsposedEnabled = try {
                val markerResult = Shell.cmd("test -f $XPOSED_MARKER && echo YES || echo NO").exec()
                markerResult.out.any { it.startsWith("YES") }
            } catch (_: Exception) { false }

            isInstalled
        } catch (e: Exception) {
            isInstalled = false
            isLsposedEnabled = false
            installedVersion = "未安装"
            edition = "未知"
            false
        }
    }
}