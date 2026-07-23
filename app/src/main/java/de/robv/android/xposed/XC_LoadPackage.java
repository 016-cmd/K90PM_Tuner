package de.robv.android.xposed;

/**
 * Stub: XC_LoadPackage — Xposed callback container.
 */
public class XC_LoadPackage {

    public static class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public boolean isFirstApplication;
    }
}