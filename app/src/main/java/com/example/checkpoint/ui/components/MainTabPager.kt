package com.example.checkpoint.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.checkpoint.ui.screens.CreatePostScreen
import com.example.checkpoint.ui.screens.MainScreen
import com.example.checkpoint.ui.screens.MessagesScreen
import com.example.checkpoint.ui.screens.ProfileScreen
import com.example.checkpoint.ui.screens.SearchScreen
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.UserStatus

/**
 * Swipe tra le sezioni principali dell'applicazione.
 */
@Composable
fun MainTabPager(
    pagerState: PagerState,
    tabs: List<String>,
    uiState: MainUiState,
    onStatusChange: (UserStatus) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1
    ) { page ->
        when (tabs[page]) {
            "search" -> SearchScreen()
            "add" -> CreatePostScreen()
            "home_feed" -> MainScreen(
                state = uiState,
                onNavigateToNotifications = onNavigateToNotifications
            )
            "messages" -> MessagesScreen()
            "profile" -> ProfileScreen(
                isDarkTheme = uiState.isDarkTheme,
                userStatus = uiState.userStatus,
                onStatusChange = onStatusChange,
                onThemeChange = onThemeChange,
                onLogout = onLogout
            )
        }
    }
}
