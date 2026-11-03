package com.k90pm.tuner.service

/**
 * 内核音频节点执行器.
 *
 * 通过 Runtime.exec("su -c ...") 执行 root 命令.
 * 与 Operit (此 AI APP) 相同机制——用户在 Magisk 永久授权后不再弹窗.
 * APP 代码中无 Shizuku 依赖,无第三方 root 库,极简安全.
 */
object WsaShell {

    /** 执行 root 命令（公开接口,供 ModuleDetector 使用） */
    fun execSyncCmd(cmd: String): String = execSync(cmd)

    private fun execSync(cmd: String): String {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor(); proc.destroy()
            output.trim()
        } catch (e: Exception) { "" }
    }

    fun getTinymix(id: Int): String = parseTinymix(execSync("tinymix $id 2>/dev/null"))

    /**
     * 批量读多个节点：**一次 su、一次进程**读完全部。
     *
     * 之前主页每次刷新都是「每个节点 fork 一个 su」，27 个节点 = 27 次
     * Runtime.exec + Magisk su 启动，2 秒一轮 ≈ 每秒 13 次进程创建，
     * 既费电又拖慢整机（系统输入分发都可能被拖到 ANR）。
     */
    fun getTinymixBatch(ids: List<Int>): Map<Int, String> {
        if (ids.isEmpty()) return emptyMap()
        val cmd = buildString {
            append("for i in ")
            append(ids.joinToString(" "))
            append("; do echo \"##\$i\"; tinymix \$i 2>/dev/null; done")
        }
        val raw = execSync(cmd)
        val out = HashMap<Int, String>(ids.size * 2)
        var cur = -1
        val sb = StringBuilder()

        fun flush() {
            if (cur >= 0) out[cur] = parseTinymix(sb.toString())
            sb.setLength(0)
        }

        for (line in raw.lineSequence()) {
            if (line.startsWith("##")) {
                flush()
                cur = line.substring(2).trim().toIntOrNull() ?: -1
            } else if (cur >= 0) {
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(line)
            }
        }
        flush()
        for (id in ids) if (!out.containsKey(id)) out[id] = "N/A"
        return out
    }

    private fun parseTinymix(raw: String): String {
        if (raw.isBlank()) return "N/A"
        val arrowIdx = raw.indexOf('>')
        if (arrowIdx >= 0) return raw.substring(arrowIdx + 1).takeWhile { it != ' ' && it != '\t' && it != '\n' }
        val colonIdx = raw.indexOf(':')
        if (colonIdx >= 0) {
            val afterColon = raw.substring(colonIdx + 1).trimStart()
            return afterColon.takeWhile { it != ' ' && it != '\t' && it != '(' && it != '\n' }
        }
        return raw.takeWhile { it != '\n' }.trim()
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

    /** 用户已在 Magisk 永久授权 → su 不弹窗,返回 true */
    fun hasRoot(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo OK"))
            val out = p.inputStream.bufferedReader().readText().trim()
            p.waitFor(); p.destroy()
            out.startsWith("OK")
        } catch (_: Exception) { false }
    }
}