package com.k90pm.tuner.music

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EqualizerManager private constructor() {

    companion object {
        const val TAG = "EqualizerManager"
        private const val PREFS_NAME = "eq_settings"
        private const val KEY_BAND_GAINS = "band_gains"
        private const val KEY_BASS_BOOST = "bass_boost"
        private const val KEY_VIRTUALIZER = "virtualizer"
        private const val KEY_BASS_ON = "bass_on"
        private const val KEY_VIRT_ON = "virt_on"
        private const val KEY_ENABLED = "eq_enabled"

        @Volatile private var INSTANCE: EqualizerManager? = null
        fun getInstance(): EqualizerManager =
            INSTANCE ?: synchronized(this) { INSTANCE ?: EqualizerManager().also { INSTANCE = it } }
    }

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var silentTrack: AudioTrack? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var _numberOfBands = MutableStateFlow(0)
    val numberOfBands: StateFlow<Int> get() = _numberOfBands

    private var _initDone = MutableStateFlow(false)
    val initDone: StateFlow<Boolean> get() = _initDone

    private var _bandFrequencies = MutableStateFlow(intArrayOf())
    val bandFrequencies: StateFlow<IntArray> get() = _bandFrequencies

    private var _bandLevels = MutableStateFlow(shortArrayOf())
    val bandLevels: StateFlow<ShortArray> get() = _bandLevels

    private var _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> get() = _enabled

    // BassBoost/Virtualizer 开关状态（可被 UI 读写）
    private var _bassBoostOn = MutableStateFlow(false)
    val bassBoostOn: StateFlow<Boolean> get() = _bassBoostOn
    fun setBassBoostOn(on: Boolean) { _bassBoostOn.value = on; saveSettings() }

    private var _virtualizerOn = MutableStateFlow(false)
    val virtualizerOn: StateFlow<Boolean> get() = _virtualizerOn
    fun setVirtualizerOn(on: Boolean) { _virtualizerOn.value = on; saveSettings() }

    // ── 初始化 ──

    fun initialize(context: Context) {
        appContext = context.applicationContext
        prefs = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        release()

        // 先尝试系统全局 session 0
        try {
            equalizer = Equalizer(0, 0).apply { enabled = true; initFromEqualizer(this) }
            _initDone.value = true; initBassBoostVirtualizer(0); restoreSettings()
            Log.i(TAG, "EQ on global session 0")
            return
        } catch (_: Exception) {}

        // 静默 AudioTrack — UI 能正常显示但效果不生效
        // 等播放器播放时会调用 attachToPlayer 迁移到真正的 audio session
        try {
            val minBuf = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(48000).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(minBuf, 2048)).setTransferMode(AudioTrack.MODE_STREAM).build()
            silentTrack = track
            val sid = track.audioSessionId
            equalizer = Equalizer(0, sid).apply { enabled = true; initFromEqualizer(this) }
            _initDone.value = true; initBassBoostVirtualizer(sid); restoreSettings()
            Log.i(TAG, "EQ on silent track session=$sid (waiting for player)")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed: ${e.message}", e)
            equalizer = null; silentTrack?.release(); silentTrack = null
            _initDone.value = true
        }
    }

    /**
     * 播放器开始播放时调用，把均衡器迁移到真实播放 session 才能生效
     * 保留当前设置的 bandLevels/bassBoost/virtualizer
     */
    fun attachToPlayer(playerSessionId: Int) {
        if (playerSessionId == 0) return
        val eqOld = equalizer
        val currentLevels = _bandLevels.value.clone()
        val currentBass = getBassBoostStrength()
        val currentVirt = getVirtualizerStrength()
        val wasOn = _enabled.value

        // 释放旧的
        eqOld?.release()
        bassBoost?.release()
        virtualizer?.release()

        try {
            equalizer = Equalizer(0, playerSessionId).apply {
                enabled = wasOn
                // 恢复频段
                for (i in currentLevels.indices) setBandLevel(i.toShort(), currentLevels[i])
                // 更新频率
                val n = numberOfBands.toInt()
                if (n != _numberOfBands.value) {
                    _numberOfBands.value = n
                    _bandFrequencies.value = IntArray(n) { getCenterFreq(it.toShort()) }
                }
            }
            bassBoost = BassBoost(0, playerSessionId).apply {
                if (currentBass > 0) setStrength(currentBass.toShort())
            }
            virtualizer = Virtualizer(0, playerSessionId).apply {
                if (currentVirt > 0) setStrength(currentVirt.toShort())
            }
            _bandLevels.value = currentLevels
            Log.i(TAG, "EQ migrated to player session=$playerSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "attachToPlayer failed: ${e.message}")
            // 恢复旧 equalizer
            equalizer = eqOld
        }
    }

    private fun initFromEqualizer(eq: Equalizer) {
        val n = eq.numberOfBands.toInt()
        _numberOfBands.value = n
        _bandFrequencies.value = IntArray(n) { eq.getCenterFreq(it.toShort()) }
        _bandLevels.value = ShortArray(n) // 先填 0，restoreSettings 会覆盖
        // 不设 enabled——保持用户上次设置的状态
    }

    private fun initBassBoostVirtualizer(sessionId: Int) {
        try { bassBoost = BassBoost(0, sessionId) } catch (_: Exception) {}
        try { virtualizer = Virtualizer(0, sessionId) } catch (_: Exception) {}
    }

    // ── 持久化 ──

    private fun restoreSettings() {
        val p = prefs ?: return
        val eq = equalizer ?: return
        val n = _numberOfBands.value
        if (n == 0) return

        // 恢复频段增益（直接写硬件+StateFlow，不触发 saveSettings）
        val savedGains = p.getString(KEY_BAND_GAINS, null)
        val levels = if (savedGains != null) parseGainsJson(savedGains) else ShortArray(n)
        for (i in 0 until n) eq.setBandLevel(i.toShort(), levels.getOrElse(i) { 0 })
        _bandLevels.value = levels

        // 恢复 BassBoost
        val bb = p.getInt(KEY_BASS_BOOST, 0)
        if (bb > 0) { bassBoost?.setStrength(bb.coerceIn(0, 1000).toShort()) }
        _bassBoostOn.value = p.getBoolean(KEY_BASS_ON, bb > 0)

        // 恢复 Virtualizer
        val virt = p.getInt(KEY_VIRTUALIZER, 0)
        if (virt > 0) { virtualizer?.setStrength(virt.coerceIn(0, 1000).toShort()) }
        _virtualizerOn.value = p.getBoolean(KEY_VIRT_ON, virt > 0)

        // 恢复总开关状态
        val wasEnabled = p.getBoolean(KEY_ENABLED, false)
        _enabled.value = wasEnabled
        if (wasEnabled) {
            equalizer?.enabled = true
            bassBoost?.enabled = _bassBoostOn.value
            virtualizer?.enabled = _virtualizerOn.value
        }

        Log.d(TAG, "Restored: bands=${levels.size} bass=$bb(${_bassBoostOn.value}) virt=$virt(${_virtualizerOn.value})")
    }

    private fun saveSettings() {
        val p = prefs ?: return
        p.edit()
            .putString(KEY_BAND_GAINS, exportCurrentGains())
            .putInt(KEY_BASS_BOOST, getBassBoostStrength())
            .putInt(KEY_VIRTUALIZER, getVirtualizerStrength())
            .putBoolean(KEY_BASS_ON, _bassBoostOn.value)
            .putBoolean(KEY_VIRT_ON, _virtualizerOn.value)
            .putBoolean(KEY_ENABLED, _enabled.value)
            .apply()
    }

    // ── 频段操作 ──

    fun setBandGain(band: Short, gainMB: Short): Boolean {
        return try {
            equalizer?.setBandLevel(band, gainMB)
            _bandLevels.value = _bandLevels.value.clone().apply { if (band.toInt() in indices) this[band.toInt()] = gainMB }
            saveSettings()
            true
        } catch (e: Exception) { Log.e(TAG, "setBandGain: ${e.message}"); false }
    }

    fun getBandGain(band: Short): Short =
        _bandLevels.value.getOrElse(band.toInt()) { 0 }

    fun setAllBandGains(gainsMB: ShortArray) {
        gainsMB.forEachIndexed { i, gain -> equalizer?.setBandLevel(i.toShort(), gain) }
        _bandLevels.value = gainsMB.clone()
        saveSettings()
    }

    fun getCurrentBandLevels(): ShortArray = _bandLevels.value.clone()
    fun getCenterFrequencies(): IntArray = _bandFrequencies.value.clone()

    // ── BassBoost ──

    fun setBassBoostStrength(strength: Int) {
        bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort())
        saveSettings()
    }

    fun getBassBoostStrength(): Int = bassBoost?.roundedStrength?.toInt() ?: 0

    // ── Virtualizer ──

    fun setVirtualizerStrength(strength: Int) {
        virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort())
        saveSettings()
    }

    fun getVirtualizerStrength(): Int = virtualizer?.roundedStrength?.toInt() ?: 0

    // ── 总开关 ──

    fun setEnabled(on: Boolean) {
        equalizer?.enabled = on
        bassBoost?.enabled = on
        virtualizer?.enabled = on
        _enabled.value = on
        saveSettings()
    }

    // ── 预设 ──

    fun applyPreset(bandGainsJson: String, bassBoostVal: Int, virtualizerVal: Int) {
        val gains = parseGainsJson(bandGainsJson)
        if (gains.isNotEmpty()) setAllBandGains(gains)
        setBassBoostStrength(bassBoostVal)
        setVirtualizerStrength(virtualizerVal)
    }

    fun exportCurrentGains(): String {
        val bands = _numberOfBands.value; if (bands == 0) return "[]"
        return _bandLevels.value.joinToString(",", "[", "]") { it.toInt().toString() }
    }

    private fun parseGainsJson(json: String): ShortArray {
        return try { json.trim('[',']').split(",").map { it.trim().toIntOrNull()?.toShort()?:0 }.toShortArray() }
        catch (_: Exception) { ShortArray(0) }
    }

    // ── 释放 ──

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { silentTrack?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null; silentTrack = null
        _enabled.value = false; _numberOfBands.value = 0
    }
}