package com.example.checkpoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checkpoint.ui.navigation.AppNavigation
import com.example.checkpoint.ui.screens.MainViewModel
import com.example.checkpoint.ui.theme.CheckPointTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            
            CheckPointTheme(
                darkTheme = uiState.isDarkTheme,
                dynamicColor = false
            ) {
                //  applica automaticamente lo sfondo e il colore del testo
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
