package com.example.checkpoint.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.state.UserStatus
import kotlinx.coroutines.launch

/**
 * Ospita la navigazione orizzontale e la barra inferiore.
 */
@Composable
fun MainContainer(
    uiState: MainUiState,
    onStatusChange: (UserStatus) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSortOrderChange: (PostSortOrder) -> Unit,
    onToggleLike: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSaveSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onCreatePost: (String, String, String?, MediaType, List<String>) -> Unit,
    onPostSuccessReset: () -> Unit,
    onLogout: () -> Unit,
    onUserClick: (UserEntity) -> Unit,
    onDismissUserPopup: () -> Unit,
    onToggleFollow: () -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToMyCheckpoints: () -> Unit,
    onTagClick: (String) -> Unit,
    onSearchSortChange: (PostSortOrder) -> Unit,
    onNavigateToLocalUsers: () -> Unit,
    onPostClick: (PostWithAuthor) -> Unit,
    onPartnerClick: (UserEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Configurazione delle tab e del pager
    val tabs = listOf("search", "add", "home_feed", "messages", "profile")
    val homeIndex = 2
    val pagerState = rememberPagerState(initialPage = homeIndex) { tabs.size }

    // Feedback per la creazione del post
    LaunchedEffect(uiState.postSuccess) {
        if (uiState.postSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar("Checkpoint created successfully!")
            }
            pagerState.animateScrollToPage(homeIndex)
            onPostSuccessReset()
        }
    }

    // Gestione del tasto indietro per tornare alla Home
    BackHandler(enabled = pagerState.currentPage != homeIndex) {
        scope.launch {
            pagerState.animateScrollToPage(homeIndex)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                currentRoute = tabs[pagerState.currentPage],
                userStatus = uiState.userStatus,
                profilePictureUri = uiState.profilePictureUri,
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

            if (uiState.isLoading && uiState.username == "Username" && uiState.posts.isEmpty() && !uiState.postSuccess) {
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
                    onThemeChange = onThemeChange,
                    onRefresh = onRefresh,
                    onSortOrderChange = onSortOrderChange,
                    onToggleLike = onToggleLike,
                    onSearch = onSearch,
                    onSaveSearch = onSaveSearch,
                    onClearHistory = onClearHistory,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    onNavigateToFollowers = onNavigateToFollowers,
                    onNavigateToMyCheckpoints = onNavigateToMyCheckpoints,
                    onTagClick = onTagClick,
                    onSearchSortChange = onSearchSortChange,
                    onNavigateToLocalUsers = onNavigateToLocalUsers,
                    onCreatePost = onCreatePost,
                    onLogout = onLogout,
                    onUserClick = onUserClick,
                    onPostClick = onPostClick,
                    onPartnerClick = onPartnerClick
                )
            }
        }
    }
}
