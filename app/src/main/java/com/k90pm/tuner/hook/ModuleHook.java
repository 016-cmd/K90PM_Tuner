package com.k90pm.tuner.hook;

import android.util.Log;

import java.io.File;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * K90PM Tuner — LSPosed 模块入口。
 *
 * 继承 libxposed API 的 XposedModule（HyperLight 同款方式）。
 * LSPosed 通过 XposedProvider + XposedModule 双条件识别并加载模块。
 *
 * onModuleLoaded 中保存 XposedInterface 引用，
 * 供 APP 层通过 isActive() 查询模块启用状态。
 */
public class ModuleHook extends XposedModule {

    private static final String TAG = "K90PM_Tuner";
    static final String MARKER_NAME = "xposed_loaded.marker";

    /** LSPosed 注入的 XposedInterface，提供 isActive() 等跨进程能力 */
    private static volatile Object savedModuleParam;

    public ModuleHook(XposedModuleInterface.ModuleLoadedParam param) {
        super(param);
        savedModuleParam = param;
        Log.i(TAG, "Module loaded by LSPosed (libxposed API)");
        writeMarker();
    }

    /** APP 层调用：检查 LSPosed 是否已加载本模块 */
    public static boolean isLoaded() {
        return savedModuleParam != null;
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        // 不 Hook 特定应用。模块通过 root shell 操作 tinymix。
    }

    private static void writeMarker() {
        try {
            File marker = new File("/data/local/tmp", MARKER_NAME);
            marker.createNewFile();
            marker.setLastModified(System.currentTimeMillis());
            Log.i(TAG, "Marker created: " + marker.getAbsolutePath() + " exists=" + marker.exists());
        } catch (Exception e) {
            Log.w(TAG, "Failed to write marker", e);
            try {
                File f2 = new File("/data/data/com.k90pm.tuner/files/" + MARKER_NAME);
                f2.createNewFile();
                Log.i(TAG, "Fallback marker created: " + f2.getAbsolutePath());
            } catch (Exception ignored) {}
        }
    }
}