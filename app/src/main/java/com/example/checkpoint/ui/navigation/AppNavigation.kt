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
import com.example.checkpoint.ui.components.MainContainer
import com.example.checkpoint.ui.screens.EditProfileScreen
import com.example.checkpoint.ui.screens.LoginScreen
import com.example.checkpoint.ui.screens.NotificationsScreen
import com.example.checkpoint.ui.screens.SignupScreen
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.viewmodel.MainViewModel

/**
 * Definizioni delle rotte per la navigazione dell'applicazione.
 */
object Screen {
    const val Login = "login"
    const val Signup = "signup"
    const val Home = "home"
    const val Notifications = "notifications"
    const val EditProfile = "edit_profile"
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
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
                onThemeChange = { viewModel.onThemeChange(it) },
                onRefresh = { viewModel.onRefresh() },
                onSortOrderChange = { viewModel.onSortOrderChange(it) },
                onToggleLike = { viewModel.onToggleLike(it) },
                onSearch = { viewModel.onSearchQueryChange(it) },
                onSaveSearch = { viewModel.onSaveSearchQuery(it) },
                onClearHistory = { viewModel.clearSearchHistory() },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile)
                },
                onCreatePost = { title, message, uri, type, muted, tags ->
                    viewModel.onCreatePost(title, message, uri, type, muted, tags)
                },
                onPostSuccessReset = { viewModel.resetPostSuccess() },
                onLogout = {
                    viewModel.onLogout()
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Home) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            EditProfileScreen(
                state = uiState,
                onBack = { navController.popBackStack() },
                onSave = { username, bio, uri ->
                    viewModel.onUpdateProfile(username, bio, uri)
                    navController.popBackStack()
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
