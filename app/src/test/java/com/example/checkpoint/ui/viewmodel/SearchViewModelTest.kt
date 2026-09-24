package com.example.checkpoint.ui.viewmodel

import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.GeoUser
import com.example.checkpoint.data.repository.PostRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private lateinit var viewModel: SearchViewModel
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakePreferencesRepository = FakeUserPreferencesRepository()
        viewModel = SearchViewModel(FeedViewModelTest.FakePostRepository(), FakeUserRepository(), fakePreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveSearch_addsToHistory() = runTest {
        val query = "new search"
        viewModel.onSaveSearchQuery(query)
        assertTrue(fakePreferencesRepository.historyList.contains(query))
    }

    class FakeUserRepository : UserRepository {
        override fun searchUsersByUsername(query: String): Flow<List<UserEntity>> = emptyFlow()
        override fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean> = emptyFlow()
        override suspend fun toggleFollow(followerEmail: String, followedEmail: String) {}
        override fun getFollowersCount(userEmail: String): Flow<Int> = emptyFlow()
        override fun getFollowingCount(userEmail: String): Flow<Int> = emptyFlow()
        override fun getFollowers(userEmail: String): Flow<List<UserEntity>> = emptyFlow()
        override fun getFollowing(userEmail: String): Flow<List<UserEntity>> = emptyFlow()
        override suspend fun updateUserLocation(userEmail: String, latitude: Double, longitude: Double) {}
        override suspend fun deleteUserLocation(userEmail: String) {}
        override suspend fun findNearbyUsers(currentUserEmail: String, latitude: Double, longitude: Double, radiusKm: Int): List<GeoUser> = emptyList()
        override suspend fun saveFcmToken(userEmail: String, token: String) {}
        override suspend fun syncUsersAndFollows(currentUserEmail: String) {}
        override suspend fun clearLocalData() {}
        override fun startRealtimeSync(currentUserEmail: String) {}
        override fun stopRealtimeSync() {}
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        val historyList = mutableListOf<String>()
        override val searchHistory = MutableStateFlow<List<String>>(emptyList())
        override val isLoggedIn = MutableStateFlow(true)
        override val userEmail = MutableStateFlow("test@gmail.com")
        override val isDarkTheme = MutableStateFlow(true)
        override val isLocationVisible = MutableStateFlow(true)
        override val hasCompletedOnboarding = MutableStateFlow(true)
        override suspend fun saveSession(email: String) {}
        override suspend fun setTheme(isDark: Boolean) {}
        override suspend fun setLocationVisible(visible: Boolean) {}
        override suspend fun saveSearchQuery(query: String) {
            historyList.add(query)
            searchHistory.value = historyList.toList()
        }
        override suspend fun clearSearchHistory() { historyList.clear() }
        override suspend fun clearSession() {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
    }
}