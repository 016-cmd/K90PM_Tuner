package com.k90pm.tuner.service

/**
 * 内核音频节点执行器
 *
 * 通过 Runtime.exec("su -c ...") 执行 tinymix 命令。
 * 仅当用户在 Magisk/APatch 已授权后调用。
 * APP 自身不主动申请 root——用户手动去面具授权。
 */
object WsaShell {

    fun ensureRoot(): Boolean = ModuleDetector.checkRoot()

    private fun execSync(cmd: String): String {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor(); proc.destroy()
            output.trim()
        } catch (e: Exception) { "" }
    }

    fun getTinymix(id: Int): String {
        val raw = execSync("tinymix $id 2>/dev/null")
        if (raw.isBlank()) return "N/A"
        val arrowIdx = raw.indexOf('>')
        if (arrowIdx >= 0) return raw.substring(arrowIdx + 1).takeWhile { it != ' ' && it != '\t' && it != '\n' }
        val colonIdx = raw.indexOf(':')
        if (colonIdx >= 0) {
            val afterColon = raw.substring(colonIdx + 1).trimStart()
            return afterColon.takeWhile { it != ' ' && it != '\t' && it != '(' && it != '\n' }
        }
        return raw
    }

    fun setTinymix(id: Int, value: String): Boolean {
        val normalized = when {
            value.equals("On", ignoreCase = true) -> "1"
            value.equals("Off", ignoreCase = true) -> "0"
            else -> value
        }
        val result = execSync("tinymix $id $normalized 2>&1")
        return !result.contains("Error", true) && !result.contains("invalid", true)
    }
}