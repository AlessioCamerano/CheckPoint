package com.example.checkpoint.ui.screens

import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.ui.viewmodel.AuthViewModel
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
    fun `onSignup with invalid email should show error`() = runTest {
        viewModel.onSignup("invalid@mail.com", "user", "pass1", "pass1")
        val state = viewModel.uiState.value
        assertEquals("Only @gmail.com addresses are allowed", state.errorMessage)
    }

    @Test
    fun `onSignup with weak password should show error`() = runTest {
        viewModel.onSignup("test@gmail.com", "user", "password", "password")
        val state = viewModel.uiState.value
        assertEquals("Password must contain at least one letter and one number", state.errorMessage)
    }

    @Test
    fun `onSignup with matching data should be success`() = runTest {
        viewModel.onSignup("test@gmail.com", "user", "pass1", "pass1")
        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
    }

    // Fakes
    class FakeAuthRepository : AuthRepository {
        private val users = mutableListOf<UserEntity>()
        override suspend fun isEmailTaken(email: String) = users.any { it.email == email }
        override suspend fun isUsernameTaken(username: String) = users.any { it.username == username }
        override suspend fun registerUser(user: UserEntity) { users.add(user) }
        override suspend fun loginUser(identifier: String) = users.find { it.email == identifier || it.username == identifier }
        override suspend fun updateUser(user: UserEntity) { /* Not needed for these tests */ }
        override suspend fun getUser(email: String) = users.find { it.email == email }
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val _isLoggedIn = MutableStateFlow(false)
        override val isLoggedIn: Flow<Boolean> = _isLoggedIn
        override val userEmail: Flow<String?> = emptyFlow()
        override val isDarkTheme: Flow<Boolean?> = emptyFlow()
        override suspend fun saveSession(email: String) { _isLoggedIn.value = true }
        override suspend fun setTheme(isDark: Boolean) {}
        override suspend fun clearSession() { _isLoggedIn.value = false }
    }
}
