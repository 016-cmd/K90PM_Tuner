package com.k90pm.tuner.service

import java.io.File

/**
 * K90PM 音质模块检测器
 * 
 * 检查 Magisk/KSU/AP 模块目录中是否安装了 k90pm_audio_plus。
 */
object ModuleDetector {

    private val MODULE_PATHS = listOf(
        "/data/adb/modules/k90pm_audio_plus",
        "/data/adb/ksu/modules/k90pm_audio_plus",
        "/data/adb/ap/modules/k90pm_audio_plus"
    )

    /** 是否安装了 K90PM 音质模块 */
    val isInstalled: Boolean by lazy {
        MODULE_PATHS.any { path ->
            val propFile = File(path, "module.prop")
            propFile.exists() && propFile.canRead()
        }
    }

    /** 检测到的模块版本名 */
    val installedVersion: String by lazy {
        for (path in MODULE_PATHS) {
            val propFile = File(path, "module.prop")
            if (propFile.exists()) {
                propFile.readLines().forEach { line ->
                    if (line.startsWith("version=")) {
                        return@lazy line.substringAfter("=").trim()
                    }
                }
            }
        }
        "未安装"
    }

    /** 检测到的是公开版(Standard)还是私人版(Ultra) */
    val edition: String by lazy {
        for (path in MODULE_PATHS) {
            val propFile = File(path, "module.prop")
            if (propFile.exists()) {
                propFile.readLines().forEach { line ->
                    when {
                        line.contains("Ultra", ignoreCase = true) -> return@lazy "Ultra"
                        line.contains("Standard", ignoreCase = true) -> return@lazy "Standard"
                    }
                }
            }
        }
        "未知"
    }

    /**
     * 刷新检测结果（重新读取文件系统）
     */
    fun refresh() {
        // 懒加载变量在 Kotlin 中只初始化一次，
        // 如需刷新可使用可变状态（在 ViewModel 中处理）
    }
}