package io.github.libxposed.api;

/**
 * Super class which all Xposed module entry classes should extend.
 * Entry classes will be instantiated once for each loaded module generation in a process.
 *
 * Minimal stub — 仅作 LSPosed 类型匹配用，不继承 XposedInterfaceWrapper。
 */
@SuppressWarnings("unused")
public abstract class XposedModule implements XposedModuleInterface {

    private final ModuleLoadedParam moduleParam;

    public XposedModule(ModuleLoadedParam param) {
        this.moduleParam = param;
    }

    protected ModuleLoadedParam getModuleLoadedParam() {
        return moduleParam;
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
    }
}