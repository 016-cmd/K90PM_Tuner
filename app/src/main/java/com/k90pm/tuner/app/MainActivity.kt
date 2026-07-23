package com.k90pm.tuner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.k90pm.tuner.ui.screens.MainScreen
import com.k90pm.tuner.ui.theme.K90TunerTheme
import com.topjohnwu.superuser.Shell

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 预初始化 root shell（在 setContent 之前避免 UI 线程阻塞）
        Shell.getShell { }

        enableEdgeToEdge()

        setContent {
            K90TunerTheme {
                MainScreen()
            }
        }
    }
}