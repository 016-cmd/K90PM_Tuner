package io.github.libxposed.service;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * XposedProvider — LSPosed 模块发现必需（libxposed/service 官方实现简化版）。
 * LSPosed daemon 调用 call("SendBinder", ...) 传递 Binder，返回非 null 即认定模块存活。
 */
public final class XposedProvider extends ContentProvider {

    private static final String TAG = "XposedProvider";
    private static final String SEND_BINDER = "SendBinder";

    @Override
    public boolean onCreate() {
        Log.i(TAG, "Provider created — LSPosed module registered");
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (SEND_BINDER.equals(method) && extras != null) {
            IBinder binder = extras.getBinder("binder");
            if (binder != null) {
                Log.d(TAG, "Binder received from LSPosed");
                try { binder.linkToDeath(() -> Log.w(TAG, "Service died"), 0); }
                catch (Throwable ignored) {}
            }
            return new Bundle();
        }
        return null;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}