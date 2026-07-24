package com.k90pm.tuner.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 通知监听器 — 接收媒体通知，提取专辑封面
 * 通过 root 授权后，MediaSessionManager.getActiveSessions 也不再抛 SecurityException
 */
class MediaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d("MediaNL", "Listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            if (notification.isMediaNotification() != true) return

            // 提取 largeIcon（专辑封面）
            val largeIcon: Bitmap? = try {
                val largeIconObj = notification.extras.getParcelable("android.largeIcon", Icon::class.java)
                largeIconObj?.getBitmap(512, 512)
            } catch (_: Exception) { null }

            if (largeIcon != null) {
                currentAlbumArt = largeIcon
            }
        } catch (_: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 媒体通知被清除时重置封面
        if (sbn.notification.isMediaNotification() == true) {
            currentAlbumArt = null
        }
    }

    companion object {
        @Volatile
        var currentAlbumArt: Bitmap? = null
    }
}