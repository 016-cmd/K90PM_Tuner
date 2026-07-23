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
    // tinymixId → 当前值字符串
    private val _controlValues = MutableStateFlow<Map<Int, String>>(emptyMap())
    val controlValues: StateFlow<Map<Int, String>> = _controlValues.asStateFlow()

    // ── Root 状态 ──
    private val _hasRoot = MutableStateFlow(false)
    val hasRoot: StateFlow<Boolean> = _hasRoot.asStateFlow()

    // ── 加载状态 ──
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        checkModule()
    }

    /**
     * 检测 K90PM 模块安装状态
     */
    fun checkModule() {
        viewModelScope.launch(Dispatchers.IO) {
            _moduleStatus.update { it.copy(isChecking = true) }
            
            // 给 lsposed 一点加载时间
            delay(300)

            val installed = ModuleDetector.isInstalled
            val version = if (installed) ModuleDetector.installedVersion else "未安装"
            val edition = if (installed) ModuleDetector.edition else "-"

            _moduleStatus.update {
                it.copy(
                    isInstalled = installed,
                    version = version,
                    edition = edition,
                    isChecking = false
                )
            }

            // 检测 root
            _hasRoot.value = WsaShell.ensureRoot()

            // 读取所有控件当前值
            if (installed && _hasRoot.value) {
                refreshAllControls()
            }
        }
    }

    /**
     * 刷新所有 WSA 控件值
     */
    fun refreshAllControls() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val newValues = mutableMapOf<Int, String>()

            coroutineScope {
                ControlRegistry.all.forEach { ctrl ->
                    launch {
                        val value = WsaShell.getTinymix(ctrl.tinymixId)
                        newValues[ctrl.tinymixId] = value
                    }
                }
            }

            _controlValues.update { newValues }
            _isRefreshing.value = false
        }
    }

    /**
     * 设置单个控件的值
     */
    fun setControl(control: WsaControl, value: String) {
        viewModelScope.launch {
            val success = WsaShell.setTinymix(control.tinymixId, value)
            if (success) {
                // 立即更新本地状态
                _controlValues.update { it + (control.tinymixId to value) }
            }
        }
    }

    /**
     * 获取控件的当前显示值
     */
    fun getValue(id: Int): String =
        _controlValues.value[id] ?: "—"

    /**
     * 控件是否可编辑（模块已安装 + 有 root）
     */
    val canEdit: Boolean
        get() = _moduleStatus.value.isInstalled && _hasRoot.value
}