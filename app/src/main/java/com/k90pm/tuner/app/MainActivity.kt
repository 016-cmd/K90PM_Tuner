package com.k90pm.tuner.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.k90pm.tuner.ui.AppContextHolder
import com.k90pm.tuner.ui.screens.MainScreen
import com.k90pm.tuner.ui.theme.K90TunerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.ctx = applicationContext
        enableEdgeToEdge()

        // Android 12+ 原生窗口模糊 — 所有透明卡片自动获得毛玻璃背景
        if (Build.VERSION.SDK_INT >= 31) {
            window.attributes = window.attributes.apply {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            }
            window.setBlurBehindRadius(36)
        }

        setContent {
            K90TunerTheme {
                MainScreen(activity = this@MainActivity)
            }
        }
    }
}