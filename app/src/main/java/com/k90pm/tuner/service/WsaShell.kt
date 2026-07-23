package com.k90pm.tuner.service

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 内核音频节点执行器
 * 
 * 通过 libsu (root) 执行 tinymix 命令，
 * 直接读写 WSA/WSA2 功放芯片寄存器。
 */
object WsaShell {

    /** 确保 root shell 可用 */
    fun ensureRoot(): Boolean =
        Shell.getShell().isRoot

    /**
     * 执行 shell 命令，返回 stdout 字符串。
     * 静默执行，不抛异常。
     */
    suspend fun exec(cmd: String): String = withContext(Dispatchers.IO) {
        try {
            val result = Shell.getShell().newJob()
                .add(cmd)
                .to(ArrayList<String>(), null)
                .exec()
            result.out.joinToString("\n").trim()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 读取 tinymix 控件的当前选中值。
     * tinymix ID 输出格式: "控件名: >值 其他可选值..." 或 "控件名: 值"
     * 返回 ">" 标记的值（当前选中项）或整行。
     */
    suspend fun getTinymix(id: Int): String {
        val raw = exec("tinymix $id 2>/dev/null")
        if (raw.isBlank()) return "N/A"

        // 提取 ">" 标记的值
        val arrowIdx = raw.indexOf('>')
        return if (arrowIdx >= 0) {
            val afterArrow = raw.substring(arrowIdx + 1)
            afterArrow.split(" ", "\t").firstOrNull { it.isNotBlank() } ?: raw
        } else {
            // 没有 ">" → 可能是 On/Off 型，取冒号后的值
            raw.substringAfter(":").trim().let {
                it.takeWhile { c -> c != '\n' && c != '\r' }
            }
        }
    }

    /**
     * 设置 tinymix 控件的值。
     * @param id 控件 ID
     * @param value 值（整数或字符串如 "On"/"Off"）
     */
    suspend fun setTinymix(id: Int, value: String): Boolean {
        val result = exec("tinymix $id '$value' 2>/dev/null")
        // tinymix 成功时通常无输出，有输出可能是错误
        return result.isEmpty() || !result.contains("Error")
    }

    /**
     * 获取控件详细信息（含可选值列表）
     */
    suspend fun getTinymixDetail(id: Int): String {
        return exec("tinymix $id 2>/dev/null")
    }
}