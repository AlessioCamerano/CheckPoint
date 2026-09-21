package com.example.checkpoint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checkpoint.ui.navigation.AppNavigation
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val launchRoute = intent?.getStringExtra("extra_target_route")

        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Aggiornamento GPS ogni volta che l'app torna in primo piano (avvio o ritorno dai permessi)
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val isGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        viewModel.refreshUserLocation(isGranted)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            CheckPointTheme(
                darkTheme = uiState.isDarkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        initialRoute = launchRoute
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
