package com.example.checkpoint.ui.viewmodel

import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.CommentWithAuthor
import com.example.checkpoint.data.local.model.GeoUser
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.MainRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.ui.state.AppNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var fakeMainRepository: FakeMainRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeMainRepository = FakeMainRepository()
        fakeAuthRepository = FakeAuthRepository()
        fakePreferencesRepository = FakeUserPreferencesRepository()
        viewModel = MainViewModel(fakeMainRepository, fakeAuthRepository, fakePreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createPost_callsRepository() = runTest {
        viewModel.onCreatePost(
            title = "Title",
            message = "Message",
            mediaUri = "uri",
            mediaType = MediaType.IMAGE,
            tags = listOf("tag1")
        )

        assertEquals(1, fakeMainRepository.posts.size)
        assertEquals("Title", fakeMainRepository.posts[0].title)
        assertEquals(MediaType.IMAGE, fakeMainRepository.posts[0].mediaType)
    }

    @Test
    fun loadData_updatesUiState() = runTest {
        val post = PostEntity(authorEmail = "test@gmail.com", title = "T", initialMessage = "M")
        fakeMainRepository.createPost(post, emptyList())

        val state = viewModel.uiState.value
        assertEquals(1, state.posts.size)
        assertEquals("T", state.posts[0].post.title)
    }

    @Test
    fun saveSearch_addsToHistory() = runTest {
        val query = "new search"
        viewModel.onSaveSearchQuery(query)

        assertTrue(fakePreferencesRepository.historyList.contains(query))
    }

    @Test
    fun notifications_areObservedAndRendered() = runTest {
        val notification = AppNotification(
            id = "n1",
            title = "Nuovo like",
            body = "Mario ti ha messo like",
            route = "home",
            timestamp = 2000L
        )
        fakeMainRepository.emitNotifications(listOf(notification))

        assertEquals(1, viewModel.uiState.value.notifications.size)
        assertEquals("Nuovo like", viewModel.uiState.value.notifications[0].title)
    }

    class FakeMainRepository : MainRepository {
        val posts = mutableListOf<PostEntity>()
        private val _postsFlow = MutableStateFlow<List<PostWithAuthor>>(emptyList())
        private val _notificationsFlow = MutableStateFlow<List<AppNotification>>(emptyList())

        fun emitNotifications(notifications: List<AppNotification>) {
            _notificationsFlow.value = notifications
        }

        override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> = _postsFlow
        override suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor? {
            return _postsFlow.value.firstOrNull { it.post.id == postId }
        }
        override fun getAllUniqueTags(): Flow<List<String>> = emptyFlow()
        override fun getTrendingTags(): Flow<List<String>> = emptyFlow()
        override fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> = _postsFlow
        override fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> = _postsFlow
        override fun searchUsersByUsername(query: String): Flow<List<UserEntity>> = emptyFlow()
        override fun searchUniqueTags(query: String): Flow<List<String>> = emptyFlow()

        override suspend fun createPost(post: PostEntity, tags: List<String>) {
            posts.add(post)
            val author = UserEntity(post.authorEmail, "user")
            val currentPosts = _postsFlow.value.toMutableList()
            currentPosts.add(PostWithAuthor(post, author, emptyList(), false, 0))
            _postsFlow.value = currentPosts
        }

        override suspend fun deletePost(postId: String) {}
        override suspend fun toggleLike(postId: String, userEmail: String) {}
        override suspend fun syncDataFromFirestore(currentUserEmail: String) {}
        override suspend fun syncHighPriorityData(currentUserEmail: String) {}
        override suspend fun clearLocalData() {}
        override fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean> = emptyFlow()
        override suspend fun toggleFollow(followerEmail: String, followedEmail: String) {}
        override fun getFollowersCount(userEmail: String): Flow<Int> = emptyFlow()
        override fun getFollowingCount(userEmail: String): Flow<Int> = emptyFlow()
        override fun getFollowers(userEmail: String): Flow<List<UserEntity>> = emptyFlow()
        override fun getFollowing(userEmail: String): Flow<List<UserEntity>> = emptyFlow()
        override suspend fun updateUserLocation(userEmail: String, latitude: Double, longitude: Double) {}
        override suspend fun deleteUserLocation(userEmail: String) {}
        override suspend fun findNearbyUsers(currentUserEmail: String, latitude: Double, longitude: Double, radiusKm: Int): List<GeoUser> = emptyList()
        override fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>> = emptyFlow()
        override fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>> = emptyFlow()
        override fun getAllMessagesFlow(): Flow<List<MessageEntity>> = emptyFlow()
        override fun getAllCommentsFlow(): Flow<List<CommentWithAuthor>> = emptyFlow()
        override suspend fun sendMessage(message: MessageEntity) {}
        override fun observeNotifications(currentUserEmail: String): Flow<List<AppNotification>> = _notificationsFlow
        override suspend fun createNotification(recipientEmail: String, title: String, body: String, route: String, type: String, senderEmail: String?, targetId: String?) {}
        override suspend fun deleteNotification(notificationId: String) {}
        override suspend fun markNotificationAsRead(notificationId: String) {}
        override suspend fun clearAllNotifications(currentUserEmail: String) {}
        override suspend fun saveFcmToken(userEmail: String, token: String) {}
        override fun startRealtimeSync(currentUserEmail: String) {}
        override fun stopRealtimeSync() {}
        override fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> = emptyFlow()
        override suspend fun addComment(comment: CommentEntity) {}
        override fun getCommentCount(postId: String): Flow<Int> = emptyFlow()
    }

    class FakeAuthRepository : AuthRepository {
        override suspend fun registerUser(user: UserEntity) {}
        override suspend fun updateUser(user: UserEntity) {}
        override suspend fun getUser(email: String): UserEntity = UserEntity(email, "user")
        override suspend fun getUserByUsername(username: String): UserEntity? = null
        override suspend fun isEmailTaken(email: String) = false
        override suspend fun isUsernameTaken(username: String) = false
        override suspend fun signUpWithEmail(email: String, username: String, password: String): Result<UserEntity> = Result.success(UserEntity(email, username))
        override suspend fun signInWithEmail(email: String, password: String): Result<UserEntity> = Result.success(UserEntity(email, "user"))
        override suspend fun signInWithGoogle(idToken: String): Result<UserEntity> = Result.success(UserEntity("test@gmail.com", "user"))
        override suspend fun logout() {}
        override fun getCurrentUserEmail(): String? = "test@gmail.com"
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        override val isLoggedIn = MutableStateFlow(false)
        override val userEmail = MutableStateFlow("test@gmail.com")
        override val isDarkTheme = MutableStateFlow(true)
        val historyList = mutableListOf<String>()
        override val searchHistory = MutableStateFlow<List<String>>(emptyList())
        override val hasCompletedOnboarding = MutableStateFlow(true)

        override suspend fun saveSession(email: String) {}
        override suspend fun setTheme(isDark: Boolean) {}
        override suspend fun saveSearchQuery(query: String) {
            historyList.add(query)
            searchHistory.value = historyList.toList()
        }
        override suspend fun clearSearchHistory() {
            historyList.clear()
            searchHistory.value = emptyList()
        }
        override suspend fun clearSession() {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
    }
}
