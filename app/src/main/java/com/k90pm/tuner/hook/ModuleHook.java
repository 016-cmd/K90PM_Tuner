package com.k90pm.tuner.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * K90PM Tuner — Xposed 入口（纯 Java，LSPosed 框架要求）
 *
 * LSPosed 加载时触发。本模块不 Hook 任何系统进程，
 * 仅作为 LSPosed 模块载体存在。
 * 实际的 WSA 寄存器控制通过 App 内 libsu (root) 完成。
 */
public class ModuleHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    @Override
    public void initZygote(StartupParam startupParam) {
        // Zygote 阶段：预初始化，暂不操作
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        // 不 Hook 特定应用。
        // 模块通过自身 APP 的 root 权限直接操作内核节点。
    }
}