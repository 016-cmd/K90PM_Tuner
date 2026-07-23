package com.k90pm.tuner.service

/**
 * K90PM 音质模块 + LSPosed 启用状态检测器。
 *
 * 启动时不调 su。用户点击"激活"按钮后调用 checkRoot()，
 * 如果用户在面具已永久授权则 su 不弹窗。
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

    private fun su(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor(); p.destroy(); out.trim()
        } catch (_: Exception) { "" }
    }

    /** 用户已去面具永久授权 → su 不弹窗，返回 true */
    fun checkRoot(): Boolean = su("echo OK").startsWith("OK")

    fun detect(): Boolean {
        return try {
            val found = MODULE_PATHS.firstOrNull { su("test -f $it/module.prop && echo YES") == "YES" }
            if (found != null) {
                isInstalled = true
                val props = su("cat $found/module.prop")
                installedVersion = props.lines().firstOrNull { it.startsWith("version=") }?.substringAfter("=")?.trim() ?: "未知版本"
                edition = when {
                    props.contains("Ultra", true) -> "Ultra"
                    props.contains("Standard", true) -> "Standard"
                    else -> "未知"
                }
            } else {
                isInstalled = false; installedVersion = "未安装"; edition = "未知"
            }
            isLsposedEnabled = su("test -f $XPOSED_MARKER && echo YES") == "YES"
            isInstalled
        } catch (_: Exception) {
            isInstalled = false; isLsposedEnabled = false; false
        }
    }
}