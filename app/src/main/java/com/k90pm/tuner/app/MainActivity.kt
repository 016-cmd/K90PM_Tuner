package com.k90pm.tuner.app

import android.os.Bundle
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

        setContent {
            K90TunerTheme {
                MainScreen(activity = this@MainActivity)
            }
        }
    }
}