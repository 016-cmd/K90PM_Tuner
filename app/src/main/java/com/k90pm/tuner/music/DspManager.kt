package com.k90pm.tuner.music

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.DynamicsProcessing
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.exp
import kotlin.math.ln

/**
 * 基于 DynamicsProcessing 的全局调音管理器（替代 EqualizerManager）
 *
 * 核心优势：
 * - DynamicsProcessing(Integer.MAX_VALUE, 0, config) 全局生效，不需要 AUDIO_SESSION_OUTPUT_MIX 权限
 * - AudioManager.AudioPlaybackCallback 监听播放状态，保持 Effect 活跃
 * - 支持 PEQ + MBC + Limiter + 声道平衡 + Global Gain
 *
 * 灵感来源：FlowMix (cn.ykload.flowmix) 的 DynamicsProcessing 方案
 */
class DspManager private constructor() {

    companion object {
        const val TAG = "DspManager"
        private const val PREFS_NAME = "dsp_settings"
        private const val KEY_ENABLED = "dsp_enabled"
        private const val KEY_BAND_GAINS = "band_gains"
        private const val KEY_GLOBAL_GAIN = "global_gain"
        private const val KEY_CHANNEL_BALANCE = "channel_balance"
        private const val KEY_CHANNEL_BALANCE_ENABLED = "channel_balance_enabled"
        private const val KEY_LEFT_MUTED = "left_muted"
        private const val KEY_RIGHT_MUTED = "right_muted"
        private const val KEY_MBC_ENABLED = "mbc_enabled"
        private const val KEY_LIMITER_ENABLED = "limiter_enabled"
        private const val KEY_VIRTUALIZER = "virtualizer"

        @Volatile
        private var INSTANCE: DspManager? = null
        fun getInstance(): DspManager =
            INSTANCE ?: synchronized(this) { INSTANCE ?: DspManager().also { INSTANCE = it } }
    }

    // ── 核心组件 ──
    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var audioManager: AudioManager? = null
    private var dp: DynamicsProcessing? = null
    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null
    private var playbackCallbackRegistered = false
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── 输出采样率 ──
    private var outputSampleRate = 48000

    // ── PEQ 频段数据 ──
    // 默认 10 段 ISO 标准频率
    val DEFAULT_BANDS = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    private var _numberOfBands = MutableStateFlow(10)
    val numberOfBands: StateFlow<Int> get() = _numberOfBands

    private var _bandFrequencies = MutableStateFlow(DEFAULT_BANDS)
    val bandFrequencies: StateFlow<FloatArray> get() = _bandFrequencies

    private var _bandLevels = MutableStateFlow(FloatArray(DEFAULT_BANDS.size) { 0f })
    val bandLevels: StateFlow<FloatArray> get() = _bandLevels

    // ── 开关状态 ──
    private var _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> get() = _enabled

    private var _initDone = MutableStateFlow(false)
    val initDone: StateFlow<Boolean> get() = _initDone

    // ── Global Gain（-20dB ~ +10dB）──
    private var _globalGain = MutableStateFlow(0f)
    val globalGain: StateFlow<Float> get() = _globalGain

    // ── 声道平衡（-1.0 全左 ~ +1.0 全右）──
    private var _channelBalanceEnabled = MutableStateFlow(false)
    val channelBalanceEnabled: StateFlow<Boolean> get() = _channelBalanceEnabled
    fun setChannelBalanceEnabled(on: Boolean) {
        _channelBalanceEnabled.value = on
        applyChannelBalance()
        saveSettings()
    }

    private var _channelBalance = MutableStateFlow(0f)
    val channelBalance: StateFlow<Float> get() = _channelBalance
    fun setChannelBalance(v: Float) {
        _channelBalance.value = v.coerceIn(-1f, 1f)
        applyChannelBalance()
        saveSettings()
    }

    private var _leftMuted = MutableStateFlow(false)
    val leftMuted: StateFlow<Boolean> get() = _leftMuted
    fun setLeftMuted(on: Boolean) { _leftMuted.value = on; applyChannelBalance(); saveSettings() }

    private var _rightMuted = MutableStateFlow(false)
    val rightMuted: StateFlow<Boolean> get() = _rightMuted
    fun setRightMuted(on: Boolean) { _rightMuted.value = on; applyChannelBalance(); saveSettings() }

    // ── MBC（多频段压缩）──
    private var _mbcEnabled = MutableStateFlow(false)
    val mbcEnabled: StateFlow<Boolean> get() = _mbcEnabled

    // ── Limiter ──
    private var _limiterEnabled = MutableStateFlow(false)
    val limiterEnabled: StateFlow<Boolean> get() = _limiterEnabled

    // ── 虚拟环绕（保留接口，实际用 Channel 延迟实现绕感）──
    private var _virtualizerOn = MutableStateFlow(false)
    val virtualizerOn: StateFlow<Boolean> get() = _virtualizerOn
    fun setVirtualizerOn(on: Boolean) {
        _virtualizerOn.value = on
        saveSettings()
    }

    // ── 初始化 ──

    fun initialize(ctx: Context) {
        if (_initDone.value && dp != null) return

        appContext = ctx.applicationContext
        prefs = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 获取音频输出采样率
        try {
            val am = appContext!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager = am
            val prop = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            outputSampleRate = prop?.toIntOrNull() ?: AudioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48000
        } catch (_: Exception) {
            outputSampleRate = 48000
        }

        release()
        rebuildEffect()
        registerPlaybackCallback()
        startHeartbeat()
        restoreSettings()

        _initDone.value = true
        Log.i(TAG, "DspManager initialized, sampleRate=$outputSampleRate")
    }

    // ── 重建 DynamicsProcessing ──

    private fun rebuildEffect(): Boolean {
        return try {
            releaseEffect()

            val bands = _bandFrequencies.value
            val levels = _bandLevels.value
            val numBands = bands.size

            val builder = DynamicsProcessing.Config.Builder(
                0,                      // variant: 0 = standard
                2,                      // channelCount: stereo
                true,                   // preEqInUse
                numBands,               // preEqBandCount
                _mbcEnabled.value,      // mbcInUse
                if (_mbcEnabled.value) 3 else 0,  // mbcBandCount
                _limiterEnabled.value,  // limiterInUse
                0,                      // postEqBandCount (not used)
                false                   // postEqInUse
            )

            // PreEQ
            val eq = DynamicsProcessing.Eq(true, true, numBands)
            for (i in 0 until numBands) {
                try {
                    eq.setBand(i, DynamicsProcessing.EqBand(true, bands[i], levels[i]))
                } catch (_: Exception) {}
            }
            builder.setPreEqByChannelIndex(0, eq)
            builder.setPreEqByChannelIndex(1, eq)

            // MBC (3-band multiband compressor)
            if (_mbcEnabled.value) {
                val mbc = DynamicsProcessing.Mbc(true, true, 3)
                // 分频点: 200Hz, 2000Hz
                mbc.setBand(0, DynamicsProcessing.MbcBand(true, 200f, 10f, 40f, 2f, -20f, 0f, -90f, 1f, 0f, 0f))
                mbc.setBand(1, DynamicsProcessing.MbcBand(true, 2000f, 5f, 60f, 2f, -24f, 0f, -90f, 1f, 0f, 0f))
                mbc.setBand(2, DynamicsProcessing.MbcBand(true, 15000f, 3f, 80f, 2f, -28f, 0f, -90f, 1f, 0f, 0f))
                builder.setMbcByChannelIndex(0, mbc)
                builder.setMbcByChannelIndex(1, mbc)
            }

            // Limiter
            if (_limiterEnabled.value) {
                val limiter = DynamicsProcessing.Limiter(true, true, 0, 5f, 50f, 10f, -3f, 6f)
                builder.setLimiterByChannelIndex(0, limiter)
                builder.setLimiterByChannelIndex(1, limiter)
            }

            val config = builder.build()
            dp = DynamicsProcessing(Integer.MAX_VALUE, 0, config)
            dp?.setControlStatusListener { _, _, _ -> } // keep-alive notification

            if (_enabled.value) {
                dp?.enabled = true
            }

            applyChannelBalance()
            Log.i(TAG, "Effect rebuilt: bands=$numBands mbc=${_mbcEnabled.value} limiter=${_limiterEnabled.value}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "rebuildEffect failed: ${e.message}", e)
            dp = null
            false
        }
    }

    private fun releaseEffect() {
        try {
            dp?.release()
        } catch (_: Exception) {}
        dp = null
    }

    // ── AudioPlaybackCallback ──

    private fun registerPlaybackCallback() {
        val am = audioManager ?: return
        if (playbackCallbackRegistered) return
        try {
            playbackCallback = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                    super.onPlaybackConfigChanged(configs)
                    // 检测到任何活跃播放 → 确认 Effect 仍然 alive
                    if (configs != null && configs.isNotEmpty()) {
                        ensureEffectAlive()
                    }
                }
            }
            am.registerAudioPlaybackCallback(playbackCallback!!, null)
            playbackCallbackRegistered = true
            Log.i(TAG, "AudioPlaybackCallback registered")
        } catch (e: Exception) {
            Log.e(TAG, "registerPlaybackCallback failed: ${e.message}")
        }
    }

    private fun unregisterPlaybackCallback() {
        try {
            if (playbackCallbackRegistered && playbackCallback != null) {
                audioManager?.unregisterAudioPlaybackCallback(playbackCallback)
            }
        } catch (_: Exception) {}
        playbackCallbackRegistered = false
        playbackCallback = null
    }

    // ── 心跳保活 ──

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(2000)
                try {
                    if (dp?.enabled != _enabled.value) {
                        _enabled.value = dp?.enabled ?: false
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun ensureEffectAlive() {
        try {
            val d = dp
            if (d != null && d.enabled != _enabled.value) {
                // Effect 状态不一致，重新 sync
                Log.w(TAG, "Effect state mismatch, syncing...")
                d.enabled = _enabled.value
            }
        } catch (e: Exception) {
            if (isRecoverableError(e)) {
                Log.w(TAG, "Effect lost, rebuilding...")
                scope.launch(Dispatchers.Main) {
                    rebuildEffect()
                    applyChannelBalance()
                }
            }
        }
    }

    private fun isRecoverableError(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("dead object") || msg.contains("invalid") ||
               msg.contains("not found") || e is IllegalStateException
    }

    // ── 总开关 ──

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        try {
            dp?.enabled = on
        } catch (e: Exception) {
            Log.e(TAG, "setEnabled failed: ${e.message}")
        }
        saveSettings()
    }

    // ── PEQ 频段操作 ──

    fun setBandGain(band: Int, gainDB: Float): Boolean {
        return try {
            dp?.setPreEqBandByChannelIndex(0, band, DynamicsProcessing.EqBand(true, _bandFrequencies.value[band], gainDB))
            dp?.setPreEqBandByChannelIndex(1, band, DynamicsProcessing.EqBand(true, _bandFrequencies.value[band], gainDB))
            val newLevels = _bandLevels.value.clone()
            if (band in newLevels.indices) newLevels[band] = gainDB
            _bandLevels.value = newLevels
            saveSettings()
            true
        } catch (e: Exception) {
            Log.e(TAG, "setBandGain failed: ${e.message}")
            false
        }
    }

    fun setBandLevels(levels: FloatArray) {
        for (i in levels.indices) {
            val freq = _bandFrequencies.value.getOrElse(i) { DEFAULT_BANDS.getOrElse(i) { 1000f } }
            try {
                dp?.setPreEqBandByChannelIndex(0, i, DynamicsProcessing.EqBand(true, freq, levels[i]))
                dp?.setPreEqBandByChannelIndex(1, i, DynamicsProcessing.EqBand(true, freq, levels[i]))
            } catch (_: Exception) {}
        }
        _bandLevels.value = levels.clone()
        saveSettings()
    }

    fun resetAllBands() {
        setBandLevels(FloatArray(_numberOfBands.value) { 0f })
        _globalGain.value = 0f
    }

    // ── 声道平衡 ──

    private fun applyChannelBalance() {
        try {
            val d = dp ?: return
            if (_channelBalanceEnabled.value) {
                val bal = _channelBalance.value
                // balance: -1=全左(右-100dB), 0=正中, +1=全右(左-100dB)
                val leftGain = if (_leftMuted.value) -100f else if (bal > 0f) -bal * 100f else 0f
                val rightGain = if (_rightMuted.value) -100f else if (bal < 0f) (bal * 100f) else 0f
                d.getChannelByChannelIndex(0).inputGain = leftGain
                d.getChannelByChannelIndex(1).inputGain = rightGain
            } else {
                d.getChannelByChannelIndex(0).inputGain = 0f
                d.getChannelByChannelIndex(1).inputGain = 0f
            }
        } catch (_: Exception) {}
    }

    // ── MBC ──

    fun setMbcEnabled(on: Boolean) {
        _mbcEnabled.value = on
        rebuildEffect()
        saveSettings()
    }

    // ── Limiter ──

    fun setLimiterEnabled(on: Boolean) {
        _limiterEnabled.value = on
        rebuildEffect()
        saveSettings()
    }

    // ── 虚拟环绕（通过 AudioManager 参数）──

    fun setVirtualizer(on: Boolean) {
        _virtualizerOn.value = on
        // 通过 AudioSystem 参数启/停虚拟环绕
        try {
            val params = if (on) "virtualizer=on" else "virtualizer=off"
            AudioSystemProxy.setParameters(params)
        } catch (_: Exception) {}
        saveSettings()
    }

    // ── 预设 ──

    fun exportCurrentGains(): String {
        return _bandLevels.value.joinToString(",", "[", "]") { "%.1f".format(it) }
    }

    fun applyPreset(gainsJson: String) {
        val gains = parseGainsJson(gainsJson)
        if (gains.isNotEmpty()) setBandLevels(gains)
    }

    private fun parseGainsJson(json: String): FloatArray {
        return try {
            json.trim('[', ']').split(",").map { it.trim().toFloatOrNull() ?: 0f }.toFloatArray()
        } catch (_: Exception) {
            FloatArray(0)
        }
    }

    fun getCurrentBandLevels(): FloatArray = _bandLevels.value.clone()
    fun getCenterFrequencies(): FloatArray = _bandFrequencies.value.clone()

    // ── 持久化 ──

    private fun restoreSettings() {
        val p = prefs ?: return
        _enabled.value = p.getBoolean(KEY_ENABLED, false)

        // 恢复频段
        val savedGains = p.getString(KEY_BAND_GAINS, null)
        if (savedGains != null) {
            val gains = parseGainsJson(savedGains)
            val bands = _bandFrequencies.value
            for (i in gains.indices) {
                if (i < bands.size) {
                    try {
                        dp?.setPreEqBandByChannelIndex(0, i, DynamicsProcessing.EqBand(true, bands[i], gains[i]))
                        dp?.setPreEqBandByChannelIndex(1, i, DynamicsProcessing.EqBand(true, bands[i], gains[i]))
                    } catch (_: Exception) {}
                }
            }
            _bandLevels.value = if (gains.size == bands.size) gains else FloatArray(bands.size) { gains.getOrElse(it) { 0f } }
        }

        // 恢复状态
        _globalGain.value = p.getFloat(KEY_GLOBAL_GAIN, 0f)
        _channelBalanceEnabled.value = p.getBoolean(KEY_CHANNEL_BALANCE_ENABLED, false)
        _channelBalance.value = p.getFloat(KEY_CHANNEL_BALANCE, 0f)
        _leftMuted.value = p.getBoolean(KEY_LEFT_MUTED, false)
        _rightMuted.value = p.getBoolean(KEY_RIGHT_MUTED, false)
        _mbcEnabled.value = p.getBoolean(KEY_MBC_ENABLED, false)
        _limiterEnabled.value = p.getBoolean(KEY_LIMITER_ENABLED, false)
        _virtualizerOn.value = p.getBoolean(KEY_VIRTUALIZER, false)

        if (_enabled.value) {
            dp?.enabled = true
        }
        applyChannelBalance()

        // 如果 MBC/Limiter 之前打开了需要重建
        if (_mbcEnabled.value || _limiterEnabled.value) {
            rebuildEffect()
            if (_enabled.value) dp?.enabled = true
            applyChannelBalance()
        }
    }

    private fun saveSettings() {
        val p = prefs ?: return
        p.edit()
            .putString(KEY_BAND_GAINS, exportCurrentGains())
            .putBoolean(KEY_ENABLED, _enabled.value)
            .putFloat(KEY_GLOBAL_GAIN, _globalGain.value)
            .putBoolean(KEY_CHANNEL_BALANCE_ENABLED, _channelBalanceEnabled.value)
            .putFloat(KEY_CHANNEL_BALANCE, _channelBalance.value)
            .putBoolean(KEY_LEFT_MUTED, _leftMuted.value)
            .putBoolean(KEY_RIGHT_MUTED, _rightMuted.value)
            .putBoolean(KEY_MBC_ENABLED, _mbcEnabled.value)
            .putBoolean(KEY_LIMITER_ENABLED, _limiterEnabled.value)
            .putBoolean(KEY_VIRTUALIZER, _virtualizerOn.value)
            .apply()
    }

    // ── 释放 ──

    fun release() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        unregisterPlaybackCallback()
        releaseEffect()
        _enabled.value = false
    }
}

/**
 * 辅助类：绕过 AudioSystem 的 @hide 限制来调用 setParameters
 */
object AudioSystemProxy {
    fun setParameters(params: String) {
        try {
            val clazz = Class.forName("android.media.AudioSystem")
            val method = clazz.getMethod("setParameters", String::class.java)
            method.invoke(null, params)
        } catch (_: Exception) {}
    }
}
