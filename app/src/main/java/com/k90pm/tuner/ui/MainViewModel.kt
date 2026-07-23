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
 * 主 ViewModel
 *
 * 管理：模块检测状态、WSA 控件实时值、root 权限状态
 *
 * 重要：APP 启动时不主动调用 su！用户需手动在 Magisk 授权后回 APP 点击"激活"。
 */
class MainViewModel : ViewModel() {

    // ── 模块检测状态 ──
    data class ModuleStatus(
        val isInstalled: Boolean = false,
        val version: String = "检测中...",
        val edition: String = "-",
        val isLsposedEnabled: Boolean = false,
        val isChecking: Boolean = true
    )

    private val _moduleStatus = MutableStateFlow(ModuleStatus())
    val moduleStatus: StateFlow<ModuleStatus> = _moduleStatus.asStateFlow()

    // ── WSA 控件实时值 ──
    private val _controlValues = MutableStateFlow<Map<Int, String>>(emptyMap())
    val controlValues: StateFlow<Map<Int, String>> = _controlValues.asStateFlow()

    // ── Root 状态（初始 unknown，等用户手动触发）──
    private val _hasRoot = MutableStateFlow<Boolean?>(null) // null = 未检测
    val hasRoot: StateFlow<Boolean?> = _hasRoot.asStateFlow()

    // ── 加载状态 ──
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // 启动时只做不需要 root 的初步检测
        checkModuleNoRoot()
    }

    /**
     * 刷新按钮：已有 root 则重新完整检测，否则仅无 root 检测
     */
    fun checkModule() {
        if (_hasRoot.value == true) {
            requestRootAndDetect()
        } else {
            checkModuleNoRoot()
        }
    }

    /**
     * 启动时检测（不碰 su，不弹窗）
     */
    private fun checkModuleNoRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            _moduleStatus.update { it.copy(isChecking = true) }

            // 仅检测 LSPosed 是否已加载过 Hook（读标记文件）
            // 如果 LSPosed 从未加载，标记文件不存在
            val lsposedOn = ModuleDetector.checkMarkerNoRoot()

            _moduleStatus.update {
                it.copy(
                    isInstalled = false,          // 需要 root 才能确认模块路径
                    version = "需授权后检测",
                    edition = "-",
                    isLsposedEnabled = lsposedOn,
                    isChecking = false
                )
            }
        }
    }

    /**
     * 用户手动触发：请求 root + 完整检测
     * 调用此方法才会触发 su → Magisk 弹窗
     */
    fun requestRootAndDetect() {
        viewModelScope.launch(Dispatchers.IO) {
            _moduleStatus.update { it.copy(isChecking = true) }

            // 这才是唯一会触发 su 的地方
            val hasRootNow = ModuleDetector.checkRootAccess()
            _hasRoot.value = hasRootNow

            if (hasRootNow) {
                val installed = ModuleDetector.detect()
                val version = if (installed) ModuleDetector.installedVersion else "未安装"
                val edition = if (installed) ModuleDetector.edition else "-"
                val lsposedOn = ModuleDetector.isLsposedEnabled

                _moduleStatus.update {
                    it.copy(
                        isInstalled = installed,
                        version = version,
                        edition = edition,
                        isLsposedEnabled = lsposedOn,
                        isChecking = false
                    )
                }

                if (canEdit) {
                    refreshAllControls()
                    startAutoRefresh()
                }
            } else {
                _moduleStatus.update { it.copy(isChecking = false) }
            }
        }
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
        get() = _moduleStatus.value.isInstalled
                && _moduleStatus.value.isLsposedEnabled
                && _hasRoot.value == true
}