package com.example.checkpoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.checkpoint.ui.navigation.AppNavigation
import com.example.checkpoint.ui.theme.CheckPointTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheckPointTheme {
                AppNavigation()
            }
        }
    }
}
