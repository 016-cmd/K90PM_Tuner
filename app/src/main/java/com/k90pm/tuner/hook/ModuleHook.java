package com.k90pm.tuner.hook;

import android.os.Environment;
import android.util.Log;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.XC_LoadPackage.LoadPackageParam;

/**
 * K90PM Tuner — Xposed 入口（纯 Java，LSPosed 框架要求）
 *
 * 当 LSPosed 启用本模块时，initZygote 会在此进程中触发。
 * 写入标记文件供 APP 读取，以判断模块是否被 LSPosed 启用。
 */
public class ModuleHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "K90PM_Tuner";
    /** 标记文件：存在即表示 LSPosed 已加载本模块 */
    static final String MARKER_NAME = "xposed_loaded.marker";

    @Override
    public void initZygote(StartupParam startupParam) {
        writeMarker();
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
    }

    /**
     * 在 APP 私有目录写入标记文件。
     * Zygote 进程环境中有 app 的 dataDir，但保险起见写到 /data/local/tmp/。
     */
    private static void writeMarker() {
        try {
            // 写入标记文件，APP 通过 root 检测是否存在
            File marker = new File("/data/local/tmp", MARKER_NAME);
            if (!marker.exists()) {
                marker.createNewFile();
                Log.i(TAG, "Marker created: LSPosed module loaded");
            }
            // 同时更新 mtime 表示最近活跃
            marker.setLastModified(System.currentTimeMillis());
        } catch (Exception e) {
            Log.w(TAG, "Failed to write marker", e);
        }
    }

    /** APP 可以通过 root 检查这个文件是否存在 */
    public static File getMarkerFile() {
        return new File("/data/local/tmp", MARKER_NAME);
    }
}