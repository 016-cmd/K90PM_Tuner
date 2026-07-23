package de.robv.android.xposed;

/**
 * Stub: IXposedHookLoadPackage — LSPosed entry point.
 * 编译时使用，运行时由 LSPosed 框架提供真实实现。
 */
public interface IXposedHookLoadPackage {
    void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
}