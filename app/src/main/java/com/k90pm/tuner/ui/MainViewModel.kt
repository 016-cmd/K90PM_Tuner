package com.k90pm.tuner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k90pm.tuner.service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 主 ViewModel — K90PM 音质模块伴生 APP。
 *
 * 管理：Magisk 模块检测状态、WSA 控件实时值、root 权限状态。
  */
class MainViewModel : ViewModel() {

    // ── 模块检测状态 ──
    data class ModuleStatus(
        val isInstalled: Boolean = false,
        val version: String = "检测中...",
        val edition: String = "-",
        val isChecking: Boolean = true
    )

    private val _moduleStatus = MutableStateFlow(ModuleStatus())
    val moduleStatus: StateFlow<ModuleStatus> = _moduleStatus.asStateFlow()

    // ── WSA 控件实时值 ──
    private val _controlValues = MutableStateFlow<Map<Int, String>>(emptyMap())
    val controlValues: StateFlow<Map<Int, String>> = _controlValues.asStateFlow()

    // ── Root 状态 ──
    private val _hasRoot = MutableStateFlow<Boolean?>(null)
    val hasRoot: StateFlow<Boolean?> = _hasRoot.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // 启动时自动尝试检测 Root（用户已在 Magisk 永久授权则不弹窗）
        autoDetect()
    }

    /** 启动时静默检测：Root 已授权则自动加载，否则等用户手动激活 */
    private fun autoDetect() {
        viewModelScope.launch(Dispatchers.IO) {
            val rootOk = WsaShell.hasRoot()
            _hasRoot.value = rootOk
            if (rootOk) {
                doDetectAndLoad()
            } else {
                _moduleStatus.update { it.copy(isChecking = false, version = "点击下方按钮激活") }
            }
        }
    }

    /** 用户手动点击激活按钮 */
    fun requestRootAndDetect() {
        viewModelScope.launch(Dispatchers.IO) {
            _moduleStatus.update { it.copy(isChecking = true) }
            val rootOk = WsaShell.hasRoot()
            _hasRoot.value = rootOk
            if (rootOk) {
                doDetectAndLoad()
            } else {
                _moduleStatus.update { it.copy(isChecking = false, version = "请先在 Magisk 中授权 Root") }
            }
        }
    }

    private fun doDetectAndLoad() {
        ModuleDetector.detect()
        _moduleStatus.update {
            it.copy(
                isInstalled = ModuleDetector.isInstalled,
                version = ModuleDetector.installedVersion,
                edition = ModuleDetector.edition,
                isChecking = false
            )
        }
        if (canEdit) { refreshAllControls(); startAutoRefresh() }
    }

    // ── 自动轮询 ──
    private var autoRefreshJob: Job? = null

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(2000)
                val newValues = mutableMapOf<Int, String>()
                ControlRegistry.all.forEach { ctrl ->
                    newValues[ctrl.tinymixId] = WsaShell.getTinymix(ctrl.tinymixId)
                }
                _controlValues.update { newValues }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun refreshAllControls() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val newValues = mutableMapOf<Int, String>()
            ControlRegistry.all.forEach { ctrl ->
                newValues[ctrl.tinymixId] = WsaShell.getTinymix(ctrl.tinymixId)
            }
            _controlValues.update { newValues }
            _isRefreshing.value = false
        }
    }

    fun setControl(control: WsaControl, value: String) {
        viewModelScope.launch {
            val success = WsaShell.setTinymix(control.tinymixId, value)
            if (success) {
                _controlValues.update { it + (control.tinymixId to value) }
            }
        }
    }

    fun getValue(id: Int): String =
        _controlValues.value[id] ?: "—"

    val canEdit: Boolean
        get() = _moduleStatus.value.isInstalled && _hasRoot.value == true
}