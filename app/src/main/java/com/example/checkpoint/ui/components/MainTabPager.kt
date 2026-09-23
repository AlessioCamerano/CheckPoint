package com.example.checkpoint.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.checkpoint.ui.screens.*
import com.example.checkpoint.ui.viewmodel.*

@Composable
fun MainTabPager(
    pagerState: PagerState,
    tabs: List<String>,
    mainViewModel: MainViewModel,
    feedViewModel: FeedViewModel,
    profileViewModel: ProfileViewModel,
    searchViewModel: SearchViewModel,
    chatViewModel: ChatViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToMyCheckpoints: () -> Unit,
    onNavigateToLocalUsers: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToPostDetail: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTag: (String) -> Unit,
    onLogout: () -> Unit
) {
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1
    ) { page ->
        when (tabs[page]) {
            "search" -> SearchScreen(
                state = searchState,
                onSearch = { searchViewModel.onSearchQueryChange(it) },
                onSaveSearch = { searchViewModel.onSaveSearchQuery(it) },
                onClearHistory = { searchViewModel.clearSearchHistory() },
                onToggleLike = { feedViewModel.onToggleLike(it) },
                onUserClick = { mainViewModel.onOpenUserPopup(it) },
                onPostClick = { feedViewModel.onOpenPostDetail(it); onNavigateToPostDetail() },
                onTagClick = { searchViewModel.onSearchByTag(it); onNavigateToTag(it) },
                onSortChange = { searchViewModel.onSearchSortOrderChange(it) }
            )
            "add" -> CreatePostScreen(
                state = feedState,
                onCreatePost = { title, msg, uri, type, tags -> feedViewModel.onCreatePost(title, msg, uri, type, tags) }
            )
            "home_feed" -> MainScreen(
                state = feedState,
                isRefreshing = mainState.isRefreshing,
                onRefresh = { mainViewModel.onRefresh() },
                onSortOrderChange = { feedViewModel.onSortOrderChange(it) },
                onToggleLike = { feedViewModel.onToggleLike(it) },
                onNavigateToNotifications = onNavigateToNotifications,
                onUserClick = { mainViewModel.onOpenUserPopup(it) },
                onPostClick = { feedViewModel.onOpenPostDetail(it); onNavigateToPostDetail() }
            )
            "messages" -> MessagesScreen(
                chatState = chatState,
                feedState = feedState,
                onPartnerClick = { chatViewModel.onOpenChat(it); onNavigateToChat() },
                onPostClick = { feedViewModel.onOpenPostDetail(it); onNavigateToPostDetail() },
                onToggleLike = { feedViewModel.onToggleLike(it) },
                onUserClick = { mainViewModel.onOpenUserPopup(it) }
            )
            "profile" -> ProfileScreen(
                isDarkTheme = mainState.isDarkTheme,
                userStatus = profileState.userStatus,
                username = profileState.username,
                profilePictureUri = profileState.profilePictureUri,
                onStatusChange = { profileViewModel.onStatusChange(it) },
                onThemeChange = { profileViewModel.onThemeChange(it) },
                onNavigateToEditProfile = onNavigateToEditProfile,
                onNavigateToFollowers = onNavigateToFollowers,
                onNavigateToMyCheckpoints = onNavigateToMyCheckpoints,
                onNavigateToLocalUsers = onNavigateToLocalUsers,
                onNavigateToOnboarding = onNavigateToOnboarding,
                onLogout = onLogout
            )
        }
    }
}