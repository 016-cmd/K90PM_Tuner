package com.k90pm.tuner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.k90pm.tuner.ui.screens.MainScreen
import com.k90pm.tuner.ui.theme.K90TunerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // APP 不主动获取 root——用户自己先去面具授权
        enableEdgeToEdge()
        setContent {
            K90TunerTheme {
                MainScreen()
            }
        }
    }
}