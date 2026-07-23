package io.github.libxposed.service;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.Objects;

public final class XposedProvider extends ContentProvider {

    private static final String TAG = "XposedProvider";

    @Override
    public boolean onCreate() {
        var targetSdk = Objects.requireNonNull(getContext()).getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && targetSdk >= Build.VERSION_CODES.R) {
            RemotePreferences.shouldNotifyCleared = true;
        }
        Log.i(TAG, "Provider created for " + getContext().getPackageName());
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method.equals("SendBinder") && extras != null) {
            IBinder binder = extras.getBinder("binder");
            if (binder != null) {
                Log.d(TAG, "binder received: " + binder);
                XposedServiceHelper.onBinderReceived(binder);
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