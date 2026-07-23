package de.robv.android.xposed;

/**
 * Stub: IXposedHookZygoteInit.
 */
public interface IXposedHookZygoteInit {

    class StartupParam {
        public String modulePath;
        public boolean startsSystemServer;
    }

    void initZygote(StartupParam startupParam) throws Throwable;
}