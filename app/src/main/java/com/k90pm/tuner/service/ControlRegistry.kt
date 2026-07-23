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
    PROTECT("保护与限幅", 4),
    ROUTING("路由配置", 5)
}

/**
 * 预定义的 WSA/WSA2 控件清单（仅 BOOL 开关 + ENUM 枚举，无滑条）
 * 
 * 数字音量等 INT 控件由模块 mixer_paths 预设，不需要 APP 实时调节。
 * tinymix ID 来源：REDMI K90 Pro Max (myron) 实测。
 */
object ControlRegistry {

    val all: List<WsaControl> = listOf(
        // ── WSA 主声道 ──
        WsaControl(195, "增益模式", WsaChip.WSA, "RX0", ControlType.ENUM, ControlCategory.GAIN,
            "压缩器增益档位",
            enumValues = listOf("G_21_DB", "G_19P5_DB", "G_18_DB", "G_16P5_DB", "G_15_DB", "G_13P5_DB", "G_12_DB", "G_10P5_DB", "G_9_DB")),
        WsaControl(196, "增益模式", WsaChip.WSA, "RX1", ControlType.ENUM, ControlCategory.GAIN,
            "压缩器增益档位",
            enumValues = listOf("G_21_DB", "G_19P5_DB", "G_18_DB", "G_16P5_DB", "G_15_DB", "G_13P5_DB", "G_12_DB", "G_10P5_DB", "G_9_DB")),

        WsaControl(207, "COMP 开关", WsaChip.WSA, "CH1", ControlType.BOOL, ControlCategory.GAIN,
            "压缩器旁路开关"),
        WsaControl(208, "COMP 开关", WsaChip.WSA, "CH2", ControlType.BOOL, ControlCategory.GAIN,
            "压缩器旁路开关"),

        WsaControl(211, "PBR 使能", WsaChip.WSA, "-", ControlType.BOOL, ControlCategory.PBR,
            "物理低音增强"),

        WsaControl(192, "VBAT 升压", WsaChip.WSA, "RX0", ControlType.BOOL, ControlCategory.POWER,
            "提升功放供电电压"),
        WsaControl(193, "VBAT 升压", WsaChip.WSA, "RX1", ControlType.BOOL, ControlCategory.POWER,
            "提升功放供电电压"),

        WsaControl(199, "Softclip", WsaChip.WSA, "RX0", ControlType.BOOL, ControlCategory.PROTECT,
            "软削波防削顶"),
        WsaControl(200, "Softclip", WsaChip.WSA, "RX1", ControlType.BOOL, ControlCategory.PROTECT,
            "软削波防削顶"),
        WsaControl(203, "数字静音", WsaChip.WSA, "RX0", ControlType.BOOL, ControlCategory.PROTECT,
            "快速静音开关"),
        WsaControl(204, "数字静音", WsaChip.WSA, "RX1", ControlType.BOOL, ControlCategory.PROTECT,
            "快速静音开关"),

        WsaControl(197, "路由切换", WsaChip.WSA, "-", ControlType.BOOL, ControlCategory.ROUTING,
            "扬声器/听筒切换"),

        // ── WSA2 低音单元 ──
        WsaControl(270, "增益模式", WsaChip.WSA2, "RX0", ControlType.ENUM, ControlCategory.GAIN,
            "压缩器增益档位",
            enumValues = listOf("G_21_DB", "G_19P5_DB", "G_18_DB", "G_16P5_DB", "G_15_DB", "G_13P5_DB", "G_12_DB", "G_10P5_DB", "G_9_DB")),
        WsaControl(271, "增益模式", WsaChip.WSA2, "RX1", ControlType.ENUM, ControlCategory.GAIN,
            "压缩器增益档位",
            enumValues = listOf("G_21_DB", "G_19P5_DB", "G_18_DB", "G_16P5_DB", "G_15_DB", "G_13P5_DB", "G_12_DB", "G_10P5_DB", "G_9_DB")),

        WsaControl(282, "COMP 开关", WsaChip.WSA2, "CH1", ControlType.BOOL, ControlCategory.GAIN,
            "压缩器旁路开关"),
        WsaControl(283, "COMP 开关", WsaChip.WSA2, "CH2", ControlType.BOOL, ControlCategory.GAIN,
            "压缩器旁路开关"),

        WsaControl(286, "PBR 使能", WsaChip.WSA2, "-", ControlType.BOOL, ControlCategory.PBR,
            "物理低音增强"),

        WsaControl(267, "VBAT 升压", WsaChip.WSA2, "RX0", ControlType.BOOL, ControlCategory.POWER,
            "提升功放供电电压"),
        WsaControl(268, "VBAT 升压", WsaChip.WSA2, "RX1", ControlType.BOOL, ControlCategory.POWER,
            "提升功放供电电压"),

        WsaControl(274, "Softclip", WsaChip.WSA2, "RX0", ControlType.BOOL, ControlCategory.PROTECT,
            "软削波防削顶"),
        WsaControl(275, "Softclip", WsaChip.WSA2, "RX1", ControlType.BOOL, ControlCategory.PROTECT,
            "软削波防削顶"),
        WsaControl(278, "数字静音", WsaChip.WSA2, "RX0", ControlType.BOOL, ControlCategory.PROTECT,
            "快速静音开关"),
        WsaControl(279, "数字静音", WsaChip.WSA2, "RX1", ControlType.BOOL, ControlCategory.PROTECT,
            "快速静音开关"),

        WsaControl(272, "路由切换", WsaChip.WSA2, "-", ControlType.BOOL, ControlCategory.ROUTING,
            "扬声器/听筒切换"),
    )

    /** 按类别分组 */
    val grouped: Map<ControlCategory, List<WsaControl>> = all.groupBy { it.category }
}