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
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.PostSortOrder
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
    onRefresh: () -> Unit,
    onSortOrderChange: (PostSortOrder) -> Unit,
    onToggleLike: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onCreatePost: (String, String, String?, MediaType, Boolean) -> Unit,
    onLogout: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1
    ) { page ->
        when (tabs[page]) {
            "search" -> SearchScreen()
            "add" -> CreatePostScreen(onCreatePost = onCreatePost)
            "home_feed" -> MainScreen(
                state = uiState,
                onRefresh = onRefresh,
                onSortOrderChange = onSortOrderChange,
                onToggleLike = onToggleLike,
                onNavigateToNotifications = onNavigateToNotifications
            )
            "messages" -> MessagesScreen()
            "profile" -> ProfileScreen(
                isDarkTheme = uiState.isDarkTheme,
                userStatus = uiState.userStatus,
                username = uiState.username,
                profilePictureUri = uiState.profilePictureUri,
                onStatusChange = onStatusChange,
                onThemeChange = onThemeChange,
                onNavigateToEditProfile = onNavigateToEditProfile,
                onLogout = onLogout
            )
        }
    }
}
