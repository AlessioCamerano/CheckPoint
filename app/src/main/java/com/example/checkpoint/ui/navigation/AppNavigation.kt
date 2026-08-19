package com.example.checkpoint.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checkpoint.ui.components.MainContainer
import com.example.checkpoint.ui.screens.LoginScreen
import com.example.checkpoint.ui.screens.MainViewModel
import com.example.checkpoint.ui.screens.NotificationsScreen
import com.example.checkpoint.ui.screens.SignupScreen

/**
 * Definizioni delle rotte per la navigazione dell'applicazione.
 */
object Screen {
    const val Login = "login"
    const val Signup = "signup"
    const val Home = "home"
    const val Notifications = "notifications"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isSessionLoaded by viewModel.isSessionLoaded.collectAsStateWithLifecycle()

    if (!isSessionLoaded) {
        // Schermata di caricamento iniziale mentre DataStore legge i dati
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Home else Screen.Login,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Screen.Login) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup)
                }
            )
        }

        composable(Screen.Signup) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MainContainer(
                uiState = uiState,
                onStatusChange = { viewModel.onStatusChange(it) },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications)
                },
                onLogout = {
                    viewModel.onLogout()
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Home) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Notifications) {
            NotificationsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
