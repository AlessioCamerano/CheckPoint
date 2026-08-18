package com.example.checkpoint.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    NavHost(
        navController = navController,
        startDestination = Screen.Login,
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
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MainContainer(
                uiState = uiState,
                onStatusChange = { viewModel.onStatusChange(it) },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications)
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
