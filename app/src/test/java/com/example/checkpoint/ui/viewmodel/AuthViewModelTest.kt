package com.example.checkpoint.ui.viewmodel

import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
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
        viewModel.onSignup("invalid@mail.com", "user", "pass1", "pass1")
        val state = viewModel.uiState.value
        assertEquals("Only @gmail.com addresses are allowed", state.errorMessage)
    }

    @Test
    fun signup_weakPassword_showsError() = runTest {
        viewModel.onSignup("test@gmail.com", "user", "password", "password")
        val state = viewModel.uiState.value
        assertEquals("Password must contain at least one letter and one number", state.errorMessage)
    }

    @Test
    fun signup_validData_isSuccess() = runTest {
        viewModel.onSignup("test@gmail.com", "user", "pass1A", "pass1A")
        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
    }

    class FakeAuthRepository : AuthRepository {
        private val users = mutableListOf<UserEntity>()
        override suspend fun registerUser(user: UserEntity) { users.add(user) }
        override suspend fun updateUser(user: UserEntity) {}
        override suspend fun getUser(email: String) = users.find { it.email == email }
        override suspend fun getUserByUsername(username: String) = users.find { it.username == username }
        override suspend fun isEmailTaken(email: String) = users.any { it.email == email }
        override suspend fun isUsernameTaken(username: String) = users.any { it.username == username }
        override suspend fun signUpWithEmail(email: String, username: String, password: String): Result<UserEntity> = Result.success(UserEntity(email, username))
        override suspend fun signInWithEmail(email: String, password: String): Result<UserEntity> = Result.success(UserEntity(email, "user"))
        override suspend fun signInWithGoogle(idToken: String): Result<UserEntity> = Result.success(UserEntity("test@gmail.com", "user"))
        override suspend fun logout() {}
        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
        override fun getCurrentUserEmail(): String? = null
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        override val isLoggedIn: Flow<Boolean> = MutableStateFlow(false)
        override val userEmail: Flow<String?> = emptyFlow()
        override val isDarkTheme: Flow<Boolean?> = emptyFlow()
        override val isLocationVisible: Flow<Boolean> = MutableStateFlow(true)
        override val searchHistory: Flow<List<String>> = emptyFlow()
        override val hasCompletedOnboarding: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun saveSession(email: String) {}
        override suspend fun setTheme(isDark: Boolean) {}
        override suspend fun setLocationVisible(visible: Boolean) {}
        override suspend fun saveSearchQuery(query: String) {}
        override suspend fun clearSearchHistory() {}
        override suspend fun clearSession() {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
    }
}