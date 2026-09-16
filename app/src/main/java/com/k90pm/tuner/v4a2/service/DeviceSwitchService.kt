package com.k90pm.tuner.v4a2.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.k90pm.tuner.v4a2.audio.AudioDevice
import com.k90pm.tuner.v4a2.audio.AudioOutputDetector
import com.k90pm.tuner.v4a2.data.ViperContainer
import com.k90pm.tuner.v4a2.data.model.DeviceSettings
import com.k90pm.tuner.v4a2.data.repository.ViperRepository
import com.k90pm.tuner.v4a2.effect.deserializeEffectPrefs
import com.k90pm.tuner.v4a2.effect.serializeEffectPrefs
import com.k90pm.tuner.v4a2.utils.FileLogger
import com.k90pm.tuner.v4a2.utils.RootShell
import com.k90pm.tuner.v4a2.viper.ConfigChannel
import com.k90pm.tuner.v4a2.viper.ViperDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

/**
 * 后台设备自动切换服务（短命，写完即关，不常驻）。
 *
 * 触发方式：系统广播（有线插拔 / 蓝牙 ACL / USB 音频）→ [DeviceSwitchReceiver]
 * → startService 拉起本服务 → 检测当前输出设备 → 从 DB 读该设备的调音预设
 * → 走 AIDL(SHM) 写回驱动 → stopSelf() 自灭。
 *
 * 与前台 MainViewModel 的关系：
 *  - 前台检测器活着时 receiver 直接忽略（互斥），保证任何时刻只有一个写者；
 *  - 本服务只负责"APP 不在前台/进程被杀"时的兜底切换，把用户"调好就退"的习惯补齐。
 */
class DeviceSwitchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val repository: ViperRepository
        get() = ViperContainer.repository()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        FileLogger.init(applicationContext)
        com.k90pm.tuner.v4a2.utils.CrashLogger.install(applicationContext)
        FileLogger.i("DeviceSwitchService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            try {
                runDeviceSwitch()
            } catch (e: Exception) {
                FileLogger.e("DeviceSwitchService", "Run failed", e)
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    /** 核心：检测当前设备 → 读 DB → 写 SHM → 结束。 */
    private suspend fun runDeviceSwitch() {
        // 冷启动进程：先手动初始化容器（Room/DataStore 尚未初始化）。
        ViperContainer.init(applicationContext)
        ConfigChannel.ensureInitialized()

        // 尊重 master 开关：若用户全局关闭了 V4A，则什么都不写。
        val masterOn = repository.getBooleanPreference(ViperRepository.PREF_MASTER_ENABLE, false).first()
        if (!masterOn) {
            FileLogger.i("DeviceSwitchService", "Master disabled, skip")
            return
        }

        // 等一小段让音频路由稳定（尤其蓝牙 ACL 刚连上时 A2DP 未必就绪），失败重试一次。
        val detector = AudioOutputDetector(applicationContext)
        var device: AudioDevice? = null
        try {
            for (attempt in 1..2) {
                delay(if (attempt == 1) 500L else 1000L)
                device = detector.activeDevice.value
                if (device != null) break
            }
        } finally {
            // 仅需一次性读取当前设备，读完立即注销回调，避免广播注册泄漏。
            detector.stop()
        }
        val current = device ?: AudioDevice.DEFAULT_SPEAKER
        FileLogger.i("DeviceSwitchService", "Detected device: ${current.id} (${current.name})")

        // 读该设备的 DB 预设；没有则用 SP 当前值落一条默认条目（保持与前台 ensureDeviceEntry 一致）。
        val saved = repository.getDeviceSettings(current.id)
        val state =
            if (saved != null) {
                FileLogger.i("DeviceSwitchService", "Load DB preset for ${current.id}")
                val base = ViperDispatcher.loadFullStateFromPrefs(repository)
                deserializeEffectPrefs(JSONObject(saved.settingsJson), base).also {
                    repository.updateDeviceLastConnected(current.id)
                }
            } else {
                FileLogger.i("DeviceSwitchService", "No DB entry for ${current.id}, snapshot current as default")
                val s = ViperDispatcher.loadFullStateFromPrefs(repository)
                repository.saveDeviceSettings(
                    DeviceSettings(
                        deviceId = current.id,
                        deviceName = current.name,
                        isHeadphone = current.isHeadphone,
                        settingsJson = serializeEffectPrefs(s).toString(),
                    ),
                )
                s
            }

        // 写回驱动（AIDL v5 SHM 路径；DDC/Convolver 需要额外 bulk 写入）
        writeFullStateWithBulk(state)
        FileLogger.i("DeviceSwitchService", "State written for ${current.id}")
    }

    /** 复刻 ViperService.writeAidlFullState：除 params 参数外，DDC/Convolver 也要写 bulk 区。 */
    private fun writeFullStateWithBulk(state: com.k90pm.tuner.v4a2.effect.EffectState) {
        ConfigChannel.writeFullState(state)
        if (state.ddc.enable && state.ddc.device.isNotEmpty()) {
            applyDdcDeviceAidl(state.ddc.device)
        }
        if (state.convolver.enable && state.convolver.kernelFile.isNotEmpty()) {
            applyConvolverKernelAidl(state.convolver.kernelFile)
        }
    }

    private fun applyDdcDeviceAidl(name: String) {
        val ddcDir = File(getExternalFilesDir(null), "DDC")
        val file = File(ddcDir, "$name.vdc")
        if (!file.exists()) {
            FileLogger.w("DeviceSwitchService", "DDC file missing: $name")
            return
        }
        val parsed = parseVdc(file) ?: return
        val sec44100 = parsed.first
        val sec48000 = parsed.second
        val perRateSize = sec44100.sumOf { it.size }
        val flat = FloatArray(perRateSize * 2)
        var off = 0
        for (sec in sec44100) {
            System.arraycopy(sec, 0, flat, off, sec.size)
            off += sec.size
        }
        for (sec in sec48000) {
            System.arraycopy(sec, 0, flat, off, sec.size)
            off += sec.size
        }
        ConfigChannel.writeBulkDdc(perRateSize, flat)
    }

    private fun applyConvolverKernelAidl(fileName: String) {
        if (fileName.isEmpty()) return
        val src = File(File(getExternalFilesDir(null), "Kernel"), fileName)
        if (!src.exists()) {
            FileLogger.w("DeviceSwitchService", "Kernel file missing: $fileName")
            return
        }
        try {
            val destDir = "/data/local/tmp/v4a/kernel"
            val destPath = "$destDir/$fileName"
            val copied = RootShell.copyFile(src, destPath)
            if (!copied) {
                FileLogger.e("DeviceSwitchService", "Kernel copy to driver failed: $destPath")
                return
            }
            ConfigChannel.writeBulkConvolverPath(destPath)
            FileLogger.i("DeviceSwitchService", "Kernel path(AIDL) written to bulk: $destPath")
        } catch (e: Exception) {
            FileLogger.e("DeviceSwitchService", "Failed to apply kernel(AIDL): $fileName", e)
        }
    }

    private fun parseVdc(file: File): Pair<List<FloatArray>, List<FloatArray>>? {
        return try {
            var coeffs44100: FloatArray? = null
            var coeffs48000: FloatArray? = null
            for (line in file.readLines()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("SR_44100:") -> {
                        coeffs44100 =
                            trimmed.removePrefix("SR_44100:").split(",").map { it.trim().toFloat() }.toFloatArray()
                    }
                    trimmed.startsWith("SR_48000:") -> {
                        coeffs48000 =
                            trimmed.removePrefix("SR_48000:").split(",").map { it.trim().toFloat() }.toFloatArray()
                    }
                }
            }
            val a = coeffs44100
            val b = coeffs48000
            if (a == null || b == null || a.isEmpty() || a.size != b.size || a.size % 5 != 0) {
                FileLogger.w("DeviceSwitchService", "DDC coefficient parse failure: ${file.name}")
                return null
            }
            a.toList().chunked(5).map { it.toFloatArray() } to
                b.toList().chunked(5).map { it.toFloatArray() }
        } catch (e: Exception) {
            FileLogger.e("DeviceSwitchService", "Failed to parse DDC: ${file.name}", e)
            null
        }
    }

    override fun onDestroy() {
        scope.cancel()
        FileLogger.i("DeviceSwitchService", "Service destroyed")
        super.onDestroy()
    }
}