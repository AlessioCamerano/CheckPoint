package com.example.checkpoint.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.checkpoint.ui.components.MainContainer
import com.example.checkpoint.ui.components.UserProfilePopup
import com.example.checkpoint.ui.screens.*
import com.example.checkpoint.ui.viewmodel.*

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

    val feedViewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
    val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val isSessionLoaded by viewModel.isSessionLoaded.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var initialIntentHandled by remember { mutableStateOf(false) }

    LaunchedEffect(isSessionLoaded, isLoggedIn, initialRoute) {
        if (!isSessionLoaded || !isLoggedIn || initialRoute.isNullOrBlank() || initialIntentHandled) return@LaunchedEffect
        if (!initialNotificationId.isNullOrBlank()) viewModel.onNotificationClickedById(initialNotificationId)

        when (initialRoute) {
            Screen.Chat, "chat" -> {
                if (!initialTargetId.isNullOrBlank()) {
                    chatViewModel.onOpenChatByEmail(initialTargetId)
                    navController.navigate(Screen.Chat) { popUpTo(Screen.Home) { inclusive = false } }
                }
            }
            Screen.PostDetail, "post_detail" -> {
                if (!initialTargetId.isNullOrBlank()) {
                    feedViewModel.onOpenPostDetailById(initialTargetId)
                    navController.navigate(Screen.PostDetail) { popUpTo(Screen.Home) { inclusive = false } }
                }
            }
            Screen.FollowersList, "followers_list" -> {
                if (!initialTargetId.isNullOrBlank()) {
                    viewModel.onOpenUserPopupByEmail(initialTargetId)
                    navController.navigate(Screen.FollowersList) { popUpTo(Screen.Home) { inclusive = false } }
                }
            }
            else -> {
                try { navController.navigate(initialRoute) { popUpTo(Screen.Home) { inclusive = false } } } catch (_: Exception) {}
            }
        }
        initialIntentHandled = true
    }

    val pendingNav by viewModel.pendingNavigation.collectAsStateWithLifecycle()
    LaunchedEffect(pendingNav, isLoggedIn) {
        val nav = pendingNav
        if (isLoggedIn && nav != null) {
            val (route, targetId, _) = nav
            when (route) {
                Screen.Chat, "chat" -> {
                    targetId?.let { chatViewModel.onOpenChatByEmail(it) }
                    navController.navigate(Screen.Chat) { popUpTo(Screen.Home) { inclusive = false } }
                }
                Screen.PostDetail, "post_detail" -> {
                    targetId?.let { feedViewModel.onOpenPostDetailById(it) }
                    navController.navigate(Screen.PostDetail) { popUpTo(Screen.Home) { inclusive = false } }
                }
                Screen.FollowersList, "followers_list" -> {
                    targetId?.let { viewModel.onOpenUserPopupByEmail(it) }
                    navController.navigate(Screen.FollowersList) { popUpTo(Screen.Home) { inclusive = false } }
                }
                Screen.Notifications, "notifications" -> navController.navigate(Screen.Notifications)
                else -> navController.navigate(Screen.Home) { popUpTo(Screen.Home) { inclusive = false } }
            }
            viewModel.consumePendingNavigation()
        }
    }

    val startDestination = when {
        initialRoute != null && !isLoggedIn -> Screen.Login
        isLoggedIn -> Screen.Home
        hasCompletedOnboarding -> Screen.Login
        else -> Screen.Onboarding
    }

    if (!isSessionLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
                        navController.navigate(Screen.Login) { popUpTo(Screen.Onboarding) { inclusive = true } }
                    }
                )
            }
            composable(Screen.OnboardingReview) {
                OnboardingScreen(showPermissionsStep = false, onFinish = { navController.popBackStack() })
            }
            composable(Screen.Login) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screen.Home) { popUpTo(Screen.Login) { inclusive = true } } },
                    onNavigateToSignup = { navController.navigate(Screen.Signup) }
                )
            }
            composable(Screen.Signup) {
                SignupScreen(
                    onSignupSuccess = { navController.navigate(Screen.Home) { popUpTo(Screen.Login) { inclusive = true } } },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Home) {
                MainContainer(
                    mainViewModel = viewModel,
                    feedViewModel = feedViewModel,
                    profileViewModel = profileViewModel,
                    searchViewModel = searchViewModel,
                    chatViewModel = chatViewModel,
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications) },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile) },
                    onNavigateToOnboarding = { navController.navigate(Screen.OnboardingReview) },
                    onNavigateToFollowers = { navController.navigate(Screen.FollowersList) },
                    onNavigateToMyCheckpoints = { navController.navigate(Screen.MyCheckpoints) },
                    onNavigateToLocalUsers = { navController.navigate(Screen.LocalUsers) },
                    onNavigateToPostDetail = { navController.navigate(Screen.PostDetail) },
                    onNavigateToChat = { navController.navigate(Screen.Chat) },
                    onNavigateToTag = { navController.navigate("tag_results/$it") },
                    onLogout = {
                        viewModel.onLogout()
                        profileViewModel.onLogout()
                        navController.navigate(Screen.Login) { popUpTo(Screen.Home) { inclusive = true } }
                    }
                )
            }
            composable(Screen.Chat) {
                val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
                val partner = chatState.selectedChatPartner
                if (partner != null) {
                    ChatScreen(
                        currentUserEmail = uiState.currentUserEmail,
                        partner = partner,
                        messages = chatState.currentChatMessages,
                        onBack = { chatViewModel.onCloseChat(); navController.popBackStack() },
                        onSendMessage = { chatViewModel.onSendMessage(it) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
            composable(Screen.PostDetail) {
                val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
                val post = feedState.selectedPost
                if (post != null) {
                    PostDetailScreen(
                        postWithAuthor = post,
                        comments = feedState.currentPostComments,
                        onBack = { feedViewModel.onClosePostDetail(); navController.popBackStack() },
                        onAddComment = { content, parentId -> feedViewModel.onAddComment(content, parentId) },
                        onToggleLike = { feedViewModel.onToggleLike(it) },
                        onUserClick = { viewModel.onOpenUserPopup(it) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
            composable(Screen.MyCheckpoints) {
                val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
                val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                val myPosts = feedState.posts.filter { it.post.authorEmail == profileState.currentUserEmail }
                MyCheckpointsScreen(
                    posts = myPosts,
                    onBack = { navController.popBackStack() },
                    onDeletePost = { feedViewModel.onDeletePost(it) },
                    onPostClick = { post ->
                        feedViewModel.onOpenPostDetail(post)
                        navController.navigate(Screen.PostDetail)
                    }
                )
            }
            composable(route = Screen.TagResults, arguments = listOf(navArgument("tag") { type = NavType.StringType })) { backStackEntry ->
                val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
                val tag = backStackEntry.arguments?.getString("tag") ?: ""
                TagResultsScreen(
                    tag = tag,
                    posts = searchState.tagResults,
                    onBack = { navController.popBackStack() },
                    onToggleLike = { feedViewModel.onToggleLike(it) },
                    onUserClick = { viewModel.onOpenUserPopup(it) },
                    onPostClick = { post ->
                        feedViewModel.onOpenPostDetail(post)
                        navController.navigate(Screen.PostDetail)
                    }
                )
            }
            composable(Screen.FollowersList) {
                val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                FollowersFollowingScreen(
                    followers = profileState.followersList,
                    following = profileState.followingList,
                    onBack = { navController.popBackStack() },
                    onUserClick = { viewModel.onOpenUserPopup(it) }
                )
            }
            composable(Screen.LocalUsers) {
                val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                LocalUsersScreen(
                    state = profileState,
                    onBack = { navController.popBackStack() },
                    onRadiusChange = { profileViewModel.onLocalUsersRadiusChange(it) },
                    onSearchNearbyUsers = { lat, lng -> profileViewModel.onSearchNearbyUsers(lat, lng) },
                    onUserClick = { viewModel.onOpenUserPopup(it) },
                    onClearLocalUsers = { profileViewModel.onClearLocalUsers() }
                )
            }
            composable(Screen.EditProfile) {
                val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                EditProfileScreen(
                    state = profileState,
                    onBack = { navController.popBackStack() },
                    onSave = { username, bio, uri ->
                        profileViewModel.onUpdateProfile(username, bio, uri)
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
                                notification.targetId?.let { chatViewModel.onOpenChatByEmail(it) }
                                navController.navigate(Screen.Chat) { popUpTo(Screen.Home) { inclusive = false } }
                            }
                            "post_detail" -> {
                                notification.targetId?.let { feedViewModel.onOpenPostDetailById(it) }
                                navController.navigate(Screen.PostDetail) { popUpTo(Screen.Home) { inclusive = false } }
                            }
                            "followers_list" -> {
                                val target = notification.targetId ?: notification.senderEmail
                                target?.let { viewModel.onOpenUserPopupByEmail(it) }
                                navController.navigate(Screen.FollowersList) { popUpTo(Screen.Home) { inclusive = false } }
                            }
                            else -> navController.navigate(Screen.Home) { popUpTo(Screen.Home) { inclusive = false } }
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
                onMessageClick = { u ->
                    chatViewModel.onOpenChat(u)
                    navController.navigate(Screen.Chat)
                }
            )
        }
    }
}