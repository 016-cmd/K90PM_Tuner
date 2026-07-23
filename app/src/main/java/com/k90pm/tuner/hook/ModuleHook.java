package com.k90pm.tuner.hook;

import android.util.Log;

import java.io.File;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * K90PM Tuner — LSPosed 模块入口。
 *
 * 继承 libxposed API 的 XposedModule（HyperLight 同款方式）。
 * LSPosed 通过 XposedProvider + XposedModule 双条件识别模块。
 */
public class ModuleHook extends XposedModule {

    private static final String TAG = "K90PM_Tuner";
    static final String MARKER_NAME = "xposed_loaded.marker";

    public ModuleHook(XposedModuleInterface.ModuleLoadedParam param) {
        super(param);
        Log.i(TAG, "Module loaded by LSPosed (libxposed API)");
        writeMarker();
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        // 不 Hook 特定应用。模块通过 root shell 操作 tinymix。
    }

    @Override
    public void onSystemServerLoaded(XposedModuleInterface.SystemServerLoadedParam param) {
        // 系统服务加载时不操作
    }

    private static void writeMarker() {
        try {
            File marker = new File("/data/local/tmp", MARKER_NAME);
            if (!marker.exists()) {
                marker.createNewFile();
                Log.i(TAG, "Marker created: LSPosed module loaded");
            }
            marker.setLastModified(System.currentTimeMillis());
        } catch (Exception e) {
            Log.w(TAG, "Failed to write marker", e);
        }
    }
}