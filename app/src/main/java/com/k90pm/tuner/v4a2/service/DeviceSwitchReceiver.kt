package com.k90pm.tuner.v4a2.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.k90pm.tuner.v4a2.utils.FileLogger

/**
 * 后台设备切换的系统广播接收器（静态注册，进程死后系统也能拉起）。
 *
 * 监听的广播：
 *  - [Intent.ACTION_HEADSET_PLUG]                    有线耳机插拔（无需权限）
 *  - [BluetoothDevice.ACTION_ACL_CONNECTED/DISCONNECTED] 蓝牙连接/断开（Android 12+ 需 BLUETOOTH_CONNECT）
 *  - [UsbManager.ACTION_USB_AUDIO_DEVICE_PLUG]       USB 音频设备插拔
 *
 * 互斥：若 MainViewModel 的检测器还活着（existingDetectorActive=true），
 * 说明 APP 进程正在前台/后台运行，由它负责切换，本 receiver 直接忽略。
 * 只有检测器不在了（进程被清理），才接管并拉起短命 [DeviceSwitchService]。
 */
class DeviceSwitchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 互斥：现有检测器活着 → 不接管
        if (existingDetectorActive) {
            FileLogger.d("DeviceSwitchReceiver", "Detector alive, skip (${intent.action})")
            return
        }

        // 防抖：同一设备状态在短时间内重复广播，忽略
        val now = System.currentTimeMillis()
        if (now - lastTriggerMs < DEBOUNCE_MS) {
            FileLogger.d("DeviceSwitchReceiver", "Debounce, skip (${intent.action})")
            return
        }
        lastTriggerMs = now

        FileLogger.i("DeviceSwitchReceiver", "Trigger: ${intent.action}")
        val service = Intent(context, DeviceSwitchService::class.java)
        try {
            context.startService(service)
        } catch (e: Exception) {
            FileLogger.e("DeviceSwitchReceiver", "startService failed", e)
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 1000L

        /** 前台检测器是否活着（MainViewModel.init 置 true，onCleared 置 false）。 */
        @Volatile
        var existingDetectorActive: Boolean = false

        @Volatile
        private var lastTriggerMs: Long = 0L
    }
}