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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
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
        // Verifica che la creazione di un checkpoint chiami il repository correttamente
        viewModel.onCreatePost("Title", "Message", "uri", MediaType.IMAGE, false, listOf("tag1"))
        
        assertEquals(1, fakeMainRepository.posts.size)
        assertEquals("Title", fakeMainRepository.posts[0].title)
        assertEquals(MediaType.IMAGE, fakeMainRepository.posts[0].mediaType)
    }

    @Test
    fun loadData_updatesUiState() = runTest {
        // Verifica che il caricamento dati aggiorni lo stato della UI
        val post = Post(authorName = "Author", authorEmail = "test@gmail.com", title = "T", initialMessage = "M")
        fakeMainRepository.createPost(post, emptyList())
        
        val state = viewModel.uiState.value
        assertEquals(1, state.posts.size)
        assertEquals("T", state.posts[0].post.title)
    }

    @Test
    fun saveSearch_addsToHistory() = runTest {
        // Verifica che il salvataggio di una ricerca la aggiunga alla cronologia
        val query = "new search"
        viewModel.onSaveSearchQuery(query)
        
        assertTrue(fakePreferencesRepository.historyList.contains(query))
    }

    @Test
    fun clearHistory_emptiesList() = runTest {
        // Verifica che la pulizia della cronologia svuoti la lista
        fakePreferencesRepository.historyList.add("query")
        viewModel.clearSearchHistory()
        
        assertTrue(fakePreferencesRepository.historyList.isEmpty())
    }

    // Classi Fake per il testing
    class FakeMainRepository : MainRepository {
        val posts = mutableListOf<Post>()
        private val _postsFlow = MutableStateFlow<List<PostWithAuthor>>(emptyList())
        
        override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> = _postsFlow
        override fun getAllUniqueTags(): Flow<List<String>> = emptyFlow()
        override fun getTrendingTags(): Flow<List<String>> = emptyFlow()
        
        override fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> = _postsFlow
        override fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> = _postsFlow
        override fun searchUsersByUsername(query: String): Flow<List<UserEntity>> = emptyFlow()

        override suspend fun createPost(post: Post, tags: List<String>) {
            posts.add(post)
            val author = UserEntity(post.authorEmail, post.authorName, "pass")
            val currentPosts = _postsFlow.value.toMutableList()
            currentPosts.add(PostWithAuthor(post, author, emptyList(), false))
            _postsFlow.value = currentPosts
        }

        override suspend fun toggleLike(postId: String, userEmail: String) {}
    }

    class FakeAuthRepository : AuthRepository {
        override suspend fun registerUser(user: UserEntity) {}
        override suspend fun updateUser(user: UserEntity) {}
        override suspend fun getUser(email: String): UserEntity = UserEntity(email, "user", "pass")
        override suspend fun isEmailTaken(email: String) = false
        override suspend fun isUsernameTaken(username: String) = false
        override suspend fun loginUser(identifier: String): UserEntity? = null
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        override val isLoggedIn = MutableStateFlow(true)
        override val userEmail = MutableStateFlow("test@gmail.com")
        override val isDarkTheme = MutableStateFlow(true)
        val historyList = mutableListOf<String>()
        override val searchHistory = MutableStateFlow<List<String>>(emptyList())
        
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
    }
}
