package com.example.checkpoint.ui.viewmodel

import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.MainRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `onCreatePost should call repository`() = runTest {
        viewModel.onCreatePost("Test Title", "Test Message", "uri", MediaType.IMAGE, false)
        
        assertEquals(1, fakeMainRepository.posts.size)
        assertEquals("Test Title", fakeMainRepository.posts[0].title)
        assertEquals(MediaType.IMAGE, fakeMainRepository.posts[0].mediaType)
    }

    @Test
    fun `loadData should update UI state with posts`() = runTest {
        val user = UserEntity("test@gmail.com", "testuser", "pass")
        val post = Post(authorName = "Author", authorEmail = "test@gmail.com", title = "T", initialMessage = "M")
        fakeMainRepository.createPost(post)
        
        val state = viewModel.uiState.value
        assertEquals(1, state.posts.size)
        assertEquals("T", state.posts[0].post.title)
    }

    // Fakes
    class FakeMainRepository : MainRepository {
        val posts = mutableListOf<Post>()
        private val _postsFlow = MutableStateFlow<List<PostWithAuthor>>(emptyList())
        
        override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> = _postsFlow
        
        override suspend fun createPost(post: Post) {
            posts.add(post)
            val author = UserEntity(post.authorEmail, post.authorName, "pass")
            val currentPosts = _postsFlow.value.toMutableList()
            currentPosts.add(PostWithAuthor(post, author, false))
            _postsFlow.value = currentPosts
        }

        override suspend fun toggleLike(postId: String, userEmail: String) {
            // Mock implementation
        }
    }

    class FakeAuthRepository : AuthRepository {
        override suspend fun registerUser(user: UserEntity) {}
        override suspend fun updateUser(user: UserEntity) {}
        override suspend fun getUser(email: String): UserEntity = UserEntity(email, "testuser", "pass")
        override suspend fun isEmailTaken(email: String) = false
        override suspend fun isUsernameTaken(username: String) = false
        override suspend fun loginUser(identifier: String): UserEntity? = null
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        override val isLoggedIn = MutableStateFlow(true)
        override val userEmail = MutableStateFlow("test@gmail.com")
        override val isDarkTheme = MutableStateFlow(true)
        override suspend fun saveSession(email: String) {}
        override suspend fun setTheme(isDark: Boolean) {}
        override suspend fun clearSession() {}
    }
}
