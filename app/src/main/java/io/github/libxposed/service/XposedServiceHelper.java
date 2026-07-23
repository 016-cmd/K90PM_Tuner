package io.github.libxposed.service;

import android.os.IBinder;

/**
 * 极简 XposedServiceHelper — 仅跟踪 lspd Binder 连接状态。
 * 当 lspd 通过 XposedProvider.call("SendBinder") 发送 Binder 时，
 * 标记 isConnected=true，Binder 死亡时标记 false。
 */
public final class XposedServiceHelper {

    private static volatile boolean sConnected = false;

    /** APP 层调用：检查 lspd 是否发送过 Binder（模块是否被 LSPosed 加载） */
    public static boolean isConnected() {
        return sConnected;
    }

    static void onBinderReceived(IBinder binder) {
        if (binder == null) return;
        try {
            sConnected = true;
            binder.linkToDeath(() -> sConnected = false, 0);
        } catch (Throwable ignored) {
            // Binder 发送太快或已经死亡
        }
    }
}