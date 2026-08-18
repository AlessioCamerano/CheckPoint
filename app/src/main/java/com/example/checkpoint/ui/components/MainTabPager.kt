package com.example.checkpoint.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.checkpoint.ui.screens.CreatePostScreen
import com.example.checkpoint.ui.screens.MainScreen
import com.example.checkpoint.ui.screens.MainUiState
import com.example.checkpoint.ui.screens.MessagesScreen
import com.example.checkpoint.ui.screens.SearchScreen

/**
 * Swipe tra le sezioni principali dell'applicazione.
 */
@Composable
fun MainTabPager(
    pagerState: PagerState,
    tabs: List<String>,
    uiState: MainUiState,
    onNavigateToNotifications: () -> Unit
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
            "profile" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Profile Screen", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
