package com.example.checkpoint.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.checkpoint.ui.screens.MainScreen

/**
 * Routes definitions for the application navigation.
 */
object Screen {
    const val Home = "home"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        composable(Screen.Home) {
            MainScreen()
        }
        
        // Add more screens here as you create them:
        // composable(Screen.Settings) { SettingsScreen() }
    }
}
