package com.example.checkpoint.ui.viewmodel

import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.CommentWithAuthor
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.data.repository.PostRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private lateinit var viewModel: FeedViewModel
    private lateinit var fakePostRepository: FakePostRepository
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakePostRepository = FakePostRepository()
        fakePreferencesRepository = FakeUserPreferencesRepository()
        viewModel = FeedViewModel(null, fakePostRepository, fakePreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createPost_updatesRepositoryAndState() = runTest {
        viewModel.onCreatePost("Title", "Message", "uri", MediaType.IMAGE, listOf("tag1"))
        
        assertEquals(1, fakePostRepository.posts.size)
        assertEquals("Title", fakePostRepository.posts[0].title)
        assertEquals(true, viewModel.uiState.value.postSuccess)
    }

    class FakePostRepository : PostRepository {
        val posts = mutableListOf<PostEntity>()
        private val _postsFlow = MutableStateFlow<List<PostWithAuthor>>(emptyList())
        override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> = _postsFlow
        override suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor? = null
        override fun getAllUniqueTags(): Flow<List<String>> = emptyFlow()
        override fun getTrendingTags(): Flow<List<String>> = emptyFlow()
        override fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> = emptyFlow()
        override fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> = emptyFlow()
        override fun searchUniqueTags(query: String): Flow<List<String>> = emptyFlow()
        
        override suspend fun createPost(post: PostEntity, tags: List<String>) {
            posts.add(post)
            val author = UserEntity(post.authorEmail, "user")
            _postsFlow.value = _postsFlow.value + PostWithAuthor(post, author)
        }
        
        override suspend fun deletePost(postId: String) {}
        override suspend fun toggleLike(postId: String, userEmail: String) {}
        override fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> = emptyFlow()
        override suspend fun addComment(comment: CommentEntity) {}
        override fun getCommentCount(postId: String): Flow<Int> = emptyFlow()
        override fun getAllCommentsFlow(): Flow<List<CommentWithAuthor>> = emptyFlow()
        override suspend fun syncPostsLikesAndComments() {}
        override fun startRealtimeSync() {}
        override fun stopRealtimeSync() {}
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        override val isLoggedIn = MutableStateFlow(true)
        override val userEmail = MutableStateFlow("test@gmail.com")
        override val isDarkTheme = MutableStateFlow(true)
        override val isLocationVisible = MutableStateFlow(true)
        override val searchHistory = MutableStateFlow<List<String>>(emptyList())
        override val hasCompletedOnboarding = MutableStateFlow(true)
        override suspend fun saveSession(email: String) {}
        override suspend fun setTheme(isDark: Boolean) {}
        override suspend fun setLocationVisible(visible: Boolean) {}
        override suspend fun saveSearchQuery(query: String) {}
        override suspend fun clearSearchHistory() {}
        override suspend fun clearSession() {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
    }
}