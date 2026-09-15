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
import com.example.checkpoint.ui.components.UserProfilePopup
import com.example.checkpoint.ui.screens.*
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.viewmodel.MainViewModel

/**
 * Definizioni delle rotte per la navigazione dell'applicazione.
 */
object Screen {
    const val Onboarding = "onboarding"
    const val OnboardingReview = "onboarding_review"
    const val Login = "login"
    const val Signup = "signup"
    const val Home = "home"
    const val Notifications = "notifications"
    const val EditProfile = "edit_profile"
    const val FollowersList = "followers_list"
    const val Chat = "chat"
    const val PostDetail = "post_detail"
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    initialRoute: String? = null
) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val isSessionLoaded by viewModel.isSessionLoaded.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = when {
        initialRoute != null && isLoggedIn -> initialRoute
        initialRoute != null && !isLoggedIn -> Screen.Login
        isLoggedIn -> Screen.Home
        hasCompletedOnboarding -> Screen.Login
        else -> Screen.Onboarding
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Onboarding) {
                OnboardingScreen(
                    showPermissionsStep = true,
                    onFinish = {
                        viewModel.onOnboardingCompleted()
                        navController.navigate(Screen.Login) {
                            popUpTo(Screen.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.OnboardingReview) {
                OnboardingScreen(
                    showPermissionsStep = false,
                    onFinish = {
                        navController.popBackStack()
                    }
                )
            }

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
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.OnboardingReview)
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
                    },
                    onUserClick = { viewModel.onOpenUserPopup(it) },
                    onDismissUserPopup = { viewModel.onDismissUserPopup() },
                    onToggleFollow = { viewModel.onToggleFollow() },
                    onNavigateToFollowers = {
                        navController.navigate(Screen.FollowersList)
                    },
                    onPostClick = { post ->
                        viewModel.onOpenPostDetail(post)
                        navController.navigate(Screen.PostDetail)
                    },
                    onPartnerClick = { partner ->
                        viewModel.onOpenChat(partner)
                        navController.navigate(Screen.Chat)
                    }
                )
            }

            composable(Screen.Chat) {
                uiState.selectedChatPartner?.let { partner ->
                    ChatScreen(
                        currentUserEmail = uiState.currentUserEmail,
                        partner = partner,
                        messages = uiState.currentChatMessages,
                        onBack = { 
                            viewModel.onCloseChat()
                            navController.popBackStack() 
                        },
                        onSendMessage = { viewModel.onSendMessage(it) }
                    )
                }
            }

            composable(Screen.PostDetail) {
                uiState.selectedPost?.let { post ->
                    PostDetailScreen(
                        postWithAuthor = post,
                        comments = uiState.currentPostComments,
                        onBack = {
                            viewModel.onClosePostDetail()
                            navController.popBackStack()
                        },
                        onAddComment = { content, parentId -> viewModel.onAddComment(content, parentId) },
                        onToggleLike = { viewModel.onToggleLike(it) }
                    )
                }
            }

            composable(Screen.FollowersList) {
                FollowersFollowingScreen(
                    followers = uiState.followersList,
                    following = uiState.followingList,
                    onBack = { navController.popBackStack() },
                    onUserClick = { user ->
                        viewModel.onOpenUserPopup(user)
                    }
                )
            }

            composable(Screen.EditProfile) {
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
                    notifications = uiState.notifications,
                    onBack = { navController.popBackStack() },
                    onNotificationClick = { notification ->
                        when (notification.route) {
                            "chat" -> {
                                uiState.chatPartners.firstOrNull()?.let { viewModel.onOpenChat(it) }
                                navController.navigate(Screen.Chat)
                            }
                            "post_detail" -> {
                                uiState.posts.firstOrNull()?.let { viewModel.onOpenPostDetail(it) }
                                navController.navigate(Screen.PostDetail)
                            }
                            "home" -> {
                                navController.navigate(Screen.Home) {
                                    popUpTo(Screen.Home) { inclusive = false }
                                }
                            }
                            else -> {
                                navController.navigate(Screen.Notifications)
                            }
                        }
                    }
                )
            }
        }

        uiState.selectedUserForPopup?.let { user ->
            UserProfilePopup(
                user = user,
                isFollowing = uiState.isFollowingSelectedUser,
                followersCount = uiState.selectedUserFollowersCount,
                followingCount = uiState.selectedUserFollowingCount,
                onDismiss = { viewModel.onDismissUserPopup() },
                onFollowClick = { viewModel.onToggleFollow() },
                onMessageClick = { user ->
                    viewModel.onOpenChat(user)
                    navController.navigate(Screen.Chat)
                }
            )
        }
    }
}
