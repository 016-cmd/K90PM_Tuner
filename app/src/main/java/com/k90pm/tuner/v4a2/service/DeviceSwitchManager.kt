package com.k90pm.tuner.v4a2.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.k90pm.tuner.v4a2.utils.FileLogger

/**
 * 后台设备自动切换开关的管理器：
 * 负责用 PackageManager.setComponentEnabledSetting 启停静态注册的 [DeviceSwitchReceiver]。
 *
 * 开关状态持久化在 SP（PREF_DEVICE_SWITCH_ENABLED），由 MainViewModel 读写；
 * 本管理器只负责"组件启停 + 权限提示文案"。
 */
object DeviceSwitchManager {

    /** 开关对应的 SP key（与 MainViewModel 共用）。 */
    const val PREF_DEVICE_SWITCH_ENABLED = "device_switch_enabled"

    fun setEnabled(context: Context, enabled: Boolean) {
        val cm = ComponentName(context, DeviceSwitchReceiver::class.java)
        val state =
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        context.packageManager.setComponentEnabledSetting(cm, state, PackageManager.DONT_KILL_APP)
        FileLogger.i("DeviceSwitchManager", "Receiver ${if (enabled) "enabled" else "disabled"}")
    }

    /** 跳转系统自启动管理页（仅提示用户手动开启，不代写）。 */
    fun openAutoStartSettings(context: Context) {
        try {
            val intent =
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            FileLogger.e("DeviceSwitchManager", "openAutoStartSettings failed", e)
        }
    }
}