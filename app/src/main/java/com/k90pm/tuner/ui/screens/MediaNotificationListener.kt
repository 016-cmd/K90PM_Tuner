package com.k90pm.tuner.ui.screens

import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 通知监听器 — 接收媒体通知，提取专辑封面
 * 通过 root 授权后，MediaSessionManager.getActiveSessions 也不再抛 SecurityException
 */
class MediaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            // 检查是否为媒体通知（通过 category 判断，兼容所有 API）
            if (notification.category != "transport") return

            // 提取 largeIcon（专辑封面）- 直接从通知中获取，不写文件
            val largeIcon: Bitmap? = try {
                @Suppress("DEPRECATION")
                notification.largeIcon?.loadDrawable(this@MediaNotificationListener)?.let { drawable ->
                    val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, 512, 512)
                    drawable.draw(canvas)
                    bmp
                }
            } catch (_: Exception) { null }

            if (largeIcon != null) {
                currentAlbumArt = largeIcon
            }
        } catch (_: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.notification.category == "transport") {
            currentAlbumArt = null
        }
    }

    companion object {
        @Volatile
        var currentAlbumArt: Bitmap? = null
    }
}