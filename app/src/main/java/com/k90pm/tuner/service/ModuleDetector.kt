package com.k90pm.tuner.service

import com.topjohnwu.superuser.Shell

/**
 * K90PM 音质模块检测器
 *
 * 使用 root shell 检测，因为 /data/adb 目录仅 root 可读。
 */
object ModuleDetector {

    private val MODULE_PATHS = listOf(
        "/data/adb/modules/k90pm_audio_plus",
        "/data/adb/ksu/modules/k90pm_audio_plus",
        "/data/adb/ap/modules/k90pm_audio_plus"
    )

    /** 是否安装了 K90PM 音质模块 */
    @Volatile var isInstalled: Boolean = false
        private set

    @Volatile var installedVersion: String = "未安装"
        private set

    @Volatile var edition: String = "未知"
        private set

    /** 执行检测（需要 root） */
    fun detect(): Boolean {
        return try {
            val result = Shell.cmd(
                MODULE_PATHS.joinToString("; ") { "test -f $it/module.prop && echo FOUND:$it" }
            ).exec()

            for (line in result.out) {
                if (line.startsWith("FOUND:")) {
                    val foundPath = line.removePrefix("FOUND:")
                    isInstalled = true

                    // 读取 module.prop 内容
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
                    return true
                }
            }
            isInstalled = false
            installedVersion = "未安装"
            edition = "未知"
            false
        } catch (e: Exception) {
            isInstalled = false
            installedVersion = "未安装"
            edition = "未知"
            false
        }
    }
}