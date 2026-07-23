package com.k90pm.tuner.service

/**
 * WSA 功放寄存器控件定义
 * 
 * 映射 tinymix 控件 ID 到 UI 可呈现的参数。
 */
data class WsaControl(
    val tinymixId: Int,
    val label: String,
    val chip: WsaChip,
    val channel: String,
    val controlType: ControlType,
    val category: ControlCategory,
    val description: String = "",
    /** 枚举型控件的可选值列表 */
    val enumValues: List<String> = emptyList(),
    /** 数值型控件的范围 */
    val range: IntRange? = null
)

enum class WsaChip(val displayName: String) {
    WSA("WSA · 主声道"),
    WSA2("WSA2 · 低音单元")
}

enum class Channel(val displayName: String) {
    RX0("左通道"),
    RX1("右通道")
}

enum class ControlType {
    ENUM,    // 多选一（comp_mode）
    BOOL,    // 开关（PBR / VBAT / Softclip / SPKRRECV）
    INT      // 整数滑块（Digital Volume）
}

enum class ControlCategory(val displayName: String, val order: Int) {
    GAIN("增益控制", 1),
    PBR("低音辐射器", 2),
    POWER("电源管理", 3),
    VOLUME("数字音量", 4),
    PROTECT("保护与限幅", 5),
    ROUTING("路由配置", 6)
}

/**
 * 预定义的 WSA/WSA2 控件清单
 * 
 * tinymix ID 来源：REDMI K90 Pro Max (myron) 实测
 */
object ControlRegistry {

    val all: List<WsaControl> = listOf(
        // ── WSA 主声道 ──
        WsaControl(195, "增益模式", WsaChip.WSA, "RX0", ControlType.ENUM, ControlCategory.GAIN,
            "comp_mode 压缩器增益档位，G_21_DB 为最大增益",
            enumValues = listOf("G_21_DB", "G_19P5_DB", "G_18_DB", "G_16P5_DB", "G_15_DB", "G_13P5_DB", "G_12_DB", "G_10P5_DB", "G_9_DB")),
        WsaControl(196, "增益模式", WsaChip.WSA, "RX1", ControlType.ENUM, ControlCategory.GAIN,
            enumValues = listOf("G_21_DB", "G_19P5_DB", "G_18_DB", "G_16P5_DB", "G_15_DB", "G_13P5_DB", "G_12_DB", "G_10P5_DB", "G_9_DB")),

        WsaControl(211, "PBR 使能", WsaChip.WSA, "-", ControlType.BOOL, ControlCategory.PBR,
            "疑似低音辐射器 (Passive Bass Radiator) 开关"),

        WsaControl(192, "VBAT 升压", WsaChip.WSA, "RX0", ControlType.BOOL, ControlCategory.POWER,
            "电池电压升压，提升功放供电电压增强瞬态响应"),
        WsaControl(193, "VBAT 升压", WsaChip.WSA, "RX1", ControlType.BOOL, ControlCategory.POWER),

        WsaControl(201, "数字音量", WsaChip.WSA, "RX0", ControlType.INT, ControlCategory.VOLUME,
            range = 0..124),
        WsaControl(202, "数字音量", WsaChip.WSA, "RX1", ControlType.INT, ControlCategory.VOLUME,
            range = 0..124),

        WsaControl(199, "Softclip", WsaChip.WSA, "RX0", ControlType.BOOL, ControlCategory.PROTECT,
            "软削波限幅，防止削顶失真"),
        WsaControl(200, "Softclip", WsaChip.WSA, "RX1", ControlType.BOOL, ControlCategory.PROTECT),

        WsaControl(197, "SPKR/RECV 路由", WsaChip.WSA, "-", ControlType.BOOL, ControlCategory.ROUTING,
            "扬声器/听筒路由切换"),

        // ── WSA2 低音单元 ──
        WsaControl(270, "增益模式", WsaChip.WSA2, "RX0", ControlType.ENUM, ControlCategory.GAIN,
            enumValues = listOf("G_21_DB", "G_19P5_DB", "G_18_DB", "G_16P5_DB", "G_15_DB", "G_13P5_DB", "G_12_DB", "G_10P5_DB", "G_9_DB")),
        WsaControl(271, "增益模式", WsaChip.WSA2, "RX1", ControlType.ENUM, ControlCategory.GAIN,
            enumValues = listOf("G_21_DB", "G_19P5_DB", "G_18_DB", "G_16P5_DB", "G_15_DB", "G_13P5_DB", "G_12_DB", "G_10P5_DB", "G_9_DB")),

        WsaControl(286, "PBR 使能", WsaChip.WSA2, "-", ControlType.BOOL, ControlCategory.PBR),

        WsaControl(267, "VBAT 升压", WsaChip.WSA2, "RX0", ControlType.BOOL, ControlCategory.POWER),
        WsaControl(268, "VBAT 升压", WsaChip.WSA2, "RX1", ControlType.BOOL, ControlCategory.POWER),

        WsaControl(276, "数字音量", WsaChip.WSA2, "RX0", ControlType.INT, ControlCategory.VOLUME,
            range = 0..124),
        WsaControl(277, "数字音量", WsaChip.WSA2, "RX1", ControlType.INT, ControlCategory.VOLUME,
            range = 0..124),

        WsaControl(274, "Softclip", WsaChip.WSA2, "RX0", ControlType.BOOL, ControlCategory.PROTECT),
        WsaControl(275, "Softclip", WsaChip.WSA2, "RX1", ControlType.BOOL, ControlCategory.PROTECT),

        WsaControl(272, "SPKR/RECV 路由", WsaChip.WSA2, "-", ControlType.BOOL, ControlCategory.ROUTING),
    )

    /** 按类别分组 */
    val grouped: Map<ControlCategory, List<WsaControl>> = all.groupBy { it.category }
}