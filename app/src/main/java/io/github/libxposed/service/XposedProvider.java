package io.github.libxposed.service;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Stub XposedProvider — LSPosed 模块发现入口。
 *
 * LSPosed 扫描声明此 provider 的 APK，然后通过
 * {@code call("SendBinder", null, extras)} 传递 Binder。
 * 我们接收 Binder 但不做进一步处理（本模块不需要 Hook 系统进程，
 * 实际控制通过 root shell 直接操作 tinymix）。
 *
 * 关键：必须实现 call() 方法，否则 LSPosed 认为模块不可用。
 */
public class XposedProvider extends ContentProvider {

    private static final String TAG = "K90PM_XposedProvider";
    private static final String SEND_BINDER = "SendBinder";

    @Override
    public boolean onCreate() {
        Log.i(TAG, "LSPosed module provider initialized");
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (SEND_BINDER.equals(method) && extras != null) {
            IBinder binder = extras.getBinder("binder");
            if (binder != null) {
                Log.i(TAG, "LSPosed binder received, module activated");
                // 不调用 XposedServiceHelper，因为我们不需要 Hook 接口
                // 仅需标记：provider.call() 返回非空表示模块存活
            }
            return new Bundle(); // 返回空 Bundle 表示接收成功
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues v, String s, String[] a) {
        return 0;
    }
}