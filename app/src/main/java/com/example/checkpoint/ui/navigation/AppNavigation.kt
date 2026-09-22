package com.example.checkpoint.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
    const val LocalUsers = "local_users"
    const val Chat = "chat"
    const val PostDetail = "post_detail"
    const val MyCheckpoints = "my_checkpoints"
    const val TagResults = "tag_results/{tag}"
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    initialRoute: String? = null,
    initialTargetId: String? = null,
    initialNotificationId: String? = null
) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val isSessionLoaded by viewModel.isSessionLoaded.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Flag per evitare di gestire l'intent iniziale più volte
    var initialIntentHandled by remember { mutableStateOf(false) }

    LaunchedEffect(isSessionLoaded, isLoggedIn, initialRoute, uiState.chatPartners, uiState.posts) {
        if (!isSessionLoaded || !isLoggedIn || initialRoute.isNullOrBlank() || initialIntentHandled) return@LaunchedEffect

        val dataReady = when (initialRoute) {
            Screen.Chat -> uiState.chatPartners.any { it.email == initialTargetId }
            Screen.PostDetail -> uiState.posts.any { it.post.id == initialTargetId }
            else -> true
        }

        if (dataReady) {
            if (!initialNotificationId.isNullOrBlank()) {
                viewModel.onNotificationClickedById(initialNotificationId)
            }
            
            when (initialRoute) {
                Screen.Chat -> {
                    if (!initialTargetId.isNullOrBlank()) {
                        viewModel.onOpenChatByEmail(initialTargetId)
                        navController.navigate(Screen.Chat) {
                            popUpTo(Screen.Home) { inclusive = false }
                        }
                    }
                }
                Screen.PostDetail -> {
                    if (!initialTargetId.isNullOrBlank()) {
                        viewModel.onOpenPostDetailById(initialTargetId)
                        navController.navigate(Screen.PostDetail) {
                            popUpTo(Screen.Home) { inclusive = false }
                        }
                    }
                }
                else -> {
                    navController.navigate(initialRoute) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                }
            }
            initialIntentHandled = true
        }
    }

    val startDestination = when {
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
                    onCreatePost = { title, message, uri, type, tags ->
                        viewModel.onCreatePost(title, message, uri, type, tags)
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
                    onNavigateToMyCheckpoints = {
                        navController.navigate(Screen.MyCheckpoints)
                    },
                    onTagClick = { tag ->
                        viewModel.onSearchByTag(tag)
                        navController.navigate("tag_results/$tag")
                    },
                    onSearchSortChange = { viewModel.onSearchSortOrderChange(it) },
                    onNavigateToLocalUsers = {
                        navController.navigate(Screen.LocalUsers)
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

            composable(Screen.MyCheckpoints) {
                val myPosts = uiState.posts.filter { it.post.authorEmail == uiState.currentUserEmail }
                MyCheckpointsScreen(
                    posts = myPosts,
                    onBack = { navController.popBackStack() },
                    onDeletePost = { viewModel.onDeletePost(it) },
                    onPostClick = { post ->
                        viewModel.onOpenPostDetail(post)
                        navController.navigate(Screen.PostDetail)
                    }
                )
            }

            composable(
                route = Screen.TagResults,
                arguments = listOf(navArgument("tag") { type = NavType.StringType })
            ) { backStackEntry ->
                val tag = backStackEntry.arguments?.getString("tag") ?: ""
                TagResultsScreen(
                    tag = tag,
                    posts = uiState.tagResults,
                    onBack = { navController.popBackStack() },
                    onToggleLike = { viewModel.onToggleLike(it) },
                    onUserClick = { viewModel.onOpenUserPopup(it) },
                    onPostClick = { post ->
                        viewModel.onOpenPostDetail(post)
                        navController.navigate(Screen.PostDetail)
                    }
                )
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

            composable(Screen.LocalUsers) {
                LocalUsersScreen(
                    state = uiState,
                    onBack = { navController.popBackStack() },
                    onRadiusChange = { viewModel.onLocalUsersRadiusChange(it) },
                    onSearchNearbyUsers = { lat, lng -> viewModel.onSearchNearbyUsers(lat, lng) },
                    onUserClick = { user -> viewModel.onOpenUserPopup(user) }
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
                    onClearAll = { viewModel.onClearAllNotifications() },
                    onNotificationClick = { notification ->
                        viewModel.onNotificationClicked(notification)
                        when (notification.route) {
                            "chat" -> {
                                val targetEmail = notification.targetId
                                if (!targetEmail.isNullOrBlank()) {
                                    viewModel.onOpenChatByEmail(targetEmail)
                                } else {
                                    uiState.chatPartners.firstOrNull()?.let { viewModel.onOpenChat(it) }
                                }
                                navController.navigate(Screen.Chat) {
                                    popUpTo(Screen.Home) { inclusive = false }
                                }
                            }
                            "post_detail" -> {
                                val targetId = notification.targetId
                                if (!targetId.isNullOrBlank()) {
                                    viewModel.onOpenPostDetailById(targetId)
                                } else {
                                    uiState.posts.firstOrNull()?.let { viewModel.onOpenPostDetail(it) }
                                }
                                navController.navigate(Screen.PostDetail) {
                                    popUpTo(Screen.Home) { inclusive = false }
                                }
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
