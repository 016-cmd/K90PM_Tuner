package io.github.libxposed.api;

import android.content.pm.ApplicationInfo;

/**
 * Minimal XposedModuleInterface — LSPosed 模块识别所需的核心接口。
 */
public interface XposedModuleInterface {

    interface ModuleLoadedParam {
        boolean isSystemServer();
        String getProcessName();
    }

    interface PackageLoadedParam {
        String getPackageName();
        ApplicationInfo getApplicationInfo();
        boolean isFirstPackage();
    }

    default void onModuleLoaded(ModuleLoadedParam param) {}
    default void onPackageLoaded(PackageLoadedParam param) {}
}