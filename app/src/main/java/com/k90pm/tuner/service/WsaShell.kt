package com.k90pm.tuner.service

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 内核音频节点执行器。
 *
 * 通过 Shizuku 获取 root shell，执行 tinymix 命令。
 * 与 Operit 相同的机制——ADB 一次授权，不再弹 su 窗口。
 */
object WsaShell {

    /** 执行 shell 命令，返回 stdout，失败返回 "" */
    private fun execSync(cmd: String): String {
        if (!Shizuku.pingBinder()) return ""
        return Shizuku.executeCommand(cmd)
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
            Shizuku.pingBinder() && Shizuku.executeCommand("echo OK").trim().startsWith("OK")
        } catch (_: Exception) { false }
    }
}