package com.example.checkpoint.ui.viewmodel

import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        fakePreferencesRepository = FakeUserPreferencesRepository()
        viewModel = AuthViewModel(fakeAuthRepository, fakePreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signup_invalidEmail_showsError() = runTest {
        // Verifica errore per email non @gmail.com
        viewModel.onSignup("invalid@mail.com", "user", "pass1", "pass1")
        val state = viewModel.uiState.value
        assertEquals("Only @gmail.com addresses are allowed", state.errorMessage)
    }

    @Test
    fun signup_weakPassword_showsError() = runTest {
        // Verifica errore per password senza numeri o lettere
        viewModel.onSignup("test@gmail.com", "user", "password", "password")
        val state = viewModel.uiState.value
        assertEquals("Password must contain at least one letter and one number", state.errorMessage)
    }

    @Test
    fun signup_validData_isSuccess() = runTest {
        // Verifica successo con dati validi
        viewModel.onSignup("test@gmail.com", "user", "pass1", "pass1")
        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
    }

    // Classi Fake per il testing
    class FakeAuthRepository : AuthRepository {
        private val users = mutableListOf<UserEntity>()
        override suspend fun isEmailTaken(email: String) = users.any { it.email == email }
        override suspend fun isUsernameTaken(username: String) = users.any { it.username == username }
        override suspend fun registerUser(user: UserEntity) { users.add(user) }
        override suspend fun loginUser(identifier: String) = users.find { it.email == identifier || it.username == identifier }
        override suspend fun updateUser(user: UserEntity) {}
        override suspend fun getUser(email: String) = users.find { it.email == email }
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val _isLoggedIn = MutableStateFlow(false)
        override val isLoggedIn: Flow<Boolean> = _isLoggedIn
        override val userEmail: Flow<String?> = emptyFlow()
        override val isDarkTheme: Flow<Boolean?> = emptyFlow()
        override val searchHistory: Flow<List<String>> = emptyFlow()
        override suspend fun saveSession(email: String) { _isLoggedIn.value = true }
        override suspend fun setTheme(isDark: Boolean) {}
        override suspend fun saveSearchQuery(query: String) {}
        override suspend fun clearSearchHistory() {}
        override suspend fun clearSession() { _isLoggedIn.value = false }
    }
}
