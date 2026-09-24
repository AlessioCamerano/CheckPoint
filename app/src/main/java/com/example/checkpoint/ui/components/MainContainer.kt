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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.checkpoint.R
import com.example.checkpoint.ui.viewmodel.ChatViewModel
import com.example.checkpoint.ui.viewmodel.FeedViewModel
import com.example.checkpoint.ui.viewmodel.MainViewModel
import com.example.checkpoint.ui.viewmodel.ProfileViewModel
import com.example.checkpoint.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

/**
 * Contenitore principale dell'interfaccia utente che gestisce la struttura dello Scaffold e il Pager delle sezioni.
 */
@Composable
fun MainContainer(
    mainViewModel: MainViewModel,
    feedViewModel: FeedViewModel,
    profileViewModel: ProfileViewModel,
    searchViewModel: SearchViewModel,
    chatViewModel: ChatViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToMyCheckpoints: () -> Unit,
    onNavigateToLocalUsers: () -> Unit,
    onNavigateToPostDetail: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTag: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()

    val tabs = listOf("search", "add", "home_feed", "messages", "profile")
    val homeIndex = 2
    val pagerState = rememberPagerState(initialPage = homeIndex) { tabs.size }
    val successMessage = stringResource(R.string.checkpoint_created_success)

    LaunchedEffect(feedState.postSuccess) {
        if (feedState.postSuccess) {
            scope.launch { snackbarHostState.showSnackbar(successMessage) }
            pagerState.animateScrollToPage(homeIndex)
            feedViewModel.resetPostSuccess()
        }
    }

    BackHandler(enabled = pagerState.currentPage != homeIndex) {
        scope.launch { pagerState.animateScrollToPage(homeIndex) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                currentRoute = tabs[pagerState.currentPage],
                userStatus = profileState.userStatus,
                profilePictureUri = profileState.profilePictureUri,
                onNavigate = { route ->
                    val index = tabs.indexOf(route)
                    if (index != -1) scope.launch { pagerState.animateScrollToPage(index) }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (feedState.isLoading && feedState.posts.isEmpty() && !feedState.postSuccess) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                MainTabPager(
                    pagerState = pagerState,
                    tabs = tabs,
                    mainViewModel = mainViewModel,
                    feedViewModel = feedViewModel,
                    profileViewModel = profileViewModel,
                    searchViewModel = searchViewModel,
                    chatViewModel = chatViewModel,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToFollowers = onNavigateToFollowers,
                    onNavigateToMyCheckpoints = onNavigateToMyCheckpoints,
                    onNavigateToLocalUsers = onNavigateToLocalUsers,
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    onNavigateToPostDetail = onNavigateToPostDetail,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToTag = onNavigateToTag,
                    onLogout = onLogout,
                )
            }
        }
    }
}
