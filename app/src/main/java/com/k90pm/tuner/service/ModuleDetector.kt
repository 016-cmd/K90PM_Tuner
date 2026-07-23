package com.k90pm.tuner.service

/**
 * K90PM 音质模块 + LSPosed 启用状态检测器
 *
 * 不主动申请 root！所有需要 root 的检测通过尝试 su 命令完成，
 * 仅在 APP 已有 root 授权（Magisk 提前授权）时才生效。
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
     * 检查是否可访问 root——用最轻量方式，
     * 不触发 su 弹窗（仅在已授权时通过）。
     */
    fun checkRootAccess(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo OK"))
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            proc.destroy()
            output.startsWith("OK")
        } catch (_: Exception) { false }
    }

    /**
     * 执行检测。
     * 如果 root 不可用则跳过，返回 false。
     */
    fun detect(): Boolean {
        if (!checkRootAccess()) {
            isInstalled = false
            isLsposedEnabled = false
            return false
        }
        return try {
            // 1. 检测模块安装
            val found = MODULE_PATHS.firstOrNull { path ->
                try {
                    val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "test -f $path/module.prop && echo YES || echo NO"))
                    val out = proc.inputStream.bufferedReader().readText()
                    proc.waitFor(); proc.destroy()
                    out.startsWith("YES")
                } catch (_: Exception) { false }
            }

            if (found != null) {
                isInstalled = true
                val proc2 = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $found/module.prop"))
                val props = proc2.inputStream.bufferedReader().readText()
                proc2.waitFor(); proc2.destroy()

                installedVersion = props.lines()
                    .firstOrNull { it.startsWith("version=") }
                    ?.substringAfter("=")?.trim() ?: "未知版本"

                edition = when {
                    props.contains("Ultra", true) -> "Ultra"
                    props.contains("Standard", true) -> "Standard"
                    else -> "未知"
                }
            } else {
                isInstalled = false
                installedVersion = "未安装"
                edition = "未知"
            }

            // 2. 检测 LSPosed 标记
            isLsposedEnabled = try {
                val proc3 = Runtime.getRuntime().exec(arrayOf("su", "-c", "test -f $XPOSED_MARKER && echo YES || echo NO"))
                val out3 = proc3.inputStream.bufferedReader().readText()
                proc3.waitFor(); proc3.destroy()
                out3.startsWith("YES")
            } catch (_: Exception) { false }

            isInstalled
        } catch (e: Exception) {
            isInstalled = false
            isLsposedEnabled = false
            false
        }
    }
}