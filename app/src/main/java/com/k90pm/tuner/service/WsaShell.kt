package com.k90pm.tuner.service

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 内核音频节点执行器。
 *
 * 通过 Shizuku newProcess 获取 root shell，执行 tinymix 命令。
 * 与 Operit 相同机制——Shizuku 一次授权后不弹窗。
 */
object WsaShell {

    /** 对外暴露的执行命令接口（供 ModuleDetector 等使用） */
    fun execSyncCmd(cmd: String): String = execSync(cmd)

    private fun execSync(cmd: String): String {
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            process.destroy()
            reader.close()
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

    fun hasShizukuRoot(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            val p = Shizuku.newProcess(arrayOf("sh", "-c", "echo OK"), null, null)
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText().trim()
            p.waitFor(); p.destroy()
            out.startsWith("OK")
        } catch (_: Exception) { false }
    }
}