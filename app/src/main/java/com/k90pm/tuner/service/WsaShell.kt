package com.k90pm.tuner.service

import com.topjohnwu.superuser.Shell

/**
 * 内核音频节点执行器
 *
 * 通过 libsu (root) 执行 tinymix 命令，
 * 直接读写 WSA/WSA2 功放芯片寄存器。
 */
object WsaShell {

    /** 确保 root shell 可用并已初始化 */
    private val rootShell: Shell by lazy {
        Shell.getShell().apply {
            // 先 ping 一下确保 root shell 就绪
            if (!isRoot) throw IllegalStateException("Root shell unavailable")
        }
    }

    /** 确保 root shell 可用 */
    fun ensureRoot(): Boolean = try {
        rootShell.isRoot
    } catch (_: Exception) { false }

    /**
     * 同步执行 shell 命令并返回 stdout 字符串。
     * 使用 libsu root shell。
     */
    private fun execSync(cmd: String): String {
        return try {
            val job = rootShell.newJob().add(cmd).to(ArrayList(), ArrayList()).exec()
            job.out.joinToString("\n").trim()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 读取 tinymix 控件的当前值。
     *
     * 输出格式:
     *   INT:  "名称: 84 (dsrange 0->124)"     → 返回 "84"
     *   BOOL: "名称: On" 或 "名称: Off"       → 返回 "On" / "Off"
     *   ENUM: "名称: >选中 其他..."           → 返回 "选中"
     */
    fun getTinymix(id: Int): String {
        val raw = execSync("tinymix $id 2>/dev/null")
        if (raw.isBlank()) return "N/A"

        // ENUM: 查找 ">" 标记
        val arrowIdx = raw.indexOf('>')
        if (arrowIdx >= 0) {
            val after = raw.substring(arrowIdx + 1)
            return after.takeWhile { it != ' ' && it != '\t' && it != '\n' }
        }

        // INT / BOOL: 取冒号后、空格或换行前的值
        val colonIdx = raw.indexOf(':')
        if (colonIdx >= 0) {
            val afterColon = raw.substring(colonIdx + 1).trimStart()
            return afterColon.takeWhile { it != ' ' && it != '\t' && it != '(' && it != '\n' }
        }

        return raw
    }

    /**
     * 设置 tinymix 控件值。
     *
     * @param id    控件 ID
     * @param value 值 — INT 传数字字符串如 "100"，BOOL 传 "1"/"0" 或 "On"/"Off"，
     *              ENUM 传枚举值如 "G_18_DB"
     */
    fun setTinymix(id: Int, value: String): Boolean {
        val normalized = when {
            value.equals("On", ignoreCase = true) -> "1"
            value.equals("Off", ignoreCase = true) -> "0"
            else -> value
        }
        val result = execSync("tinymix $id $normalized 2>&1")
        return !result.contains("Error", ignoreCase = true) && !result.contains("invalid", ignoreCase = true)
    }
}