package com.example.checkpoint.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * Ospita la navigazione orizzontale e la barra inferiore.
 */
@Composable
fun MainContainer(
    uiState: MainUiState,
    onStatusChange: (UserStatus) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Configurazione delle tab e del pager
    val tabs = listOf("search", "add", "home_feed", "messages", "profile")
    val homeIndex = 2
    val pagerState = rememberPagerState(initialPage = homeIndex) { tabs.size }

    // Gestione del tasto indietro per tornare alla Home
    BackHandler(enabled = pagerState.currentPage != homeIndex) {
        scope.launch {
            pagerState.animateScrollToPage(homeIndex)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                currentRoute = tabs[pagerState.currentPage],
                userStatus = uiState.userStatus,
                onNavigate = { route ->
                    val index = tabs.indexOf(route)
                    if (index != -1) {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                MainTabPager(
                    pagerState = pagerState,
                    tabs = tabs,
                    uiState = uiState,
                    onStatusChange = onStatusChange,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onLogout = onLogout
                )
            }
        }
    }
}
