package com.example.checkpoint.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.CheckPointApplication
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.repository.UserRepository
import com.example.checkpoint.ui.state.ProfileUiState
import com.example.checkpoint.ui.state.UserStatus
import com.example.checkpoint.util.MediaStorageManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val application: Application?,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as CheckPointApplication
                return ProfileViewModel(
                    application = application,
                    userRepository = application.container.userRepository,
                    authRepository = application.container.authRepository,
                    preferencesRepository = application.container.preferencesRepository
                ) as T
            }
        }
    }

    init {
        loadUserProfile()
        observeFollows()
        observeTheme()
        observeLocationVisibility()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (!email.isNullOrBlank()) {
                    val user = authRepository.getUser(email)
                    if (user != null) {
                        _uiState.update { it.copy(username = user.username, bio = user.bio, profilePictureUri = user.profilePictureUri, currentUserEmail = email) }
                    }
                }
            }
        }
    }

    private fun observeFollows() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email != null) {
                    launch { userRepository.getFollowers(email).collectLatest { f -> _uiState.update { it.copy(followersList = f) } } }
                    launch { userRepository.getFollowing(email).collectLatest { f -> _uiState.update { it.copy(followingList = f) } } }
                }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            preferencesRepository.isDarkTheme.collectLatest { isDark ->
                if (isDark != null) _uiState.update { it.copy(isDarkTheme = isDark) }
            }
        }
    }

    private fun observeLocationVisibility() {
        viewModelScope.launch {
            preferencesRepository.isLocationVisible.collectLatest { visible ->
                _uiState.update { it.copy(isLocationVisible = visible) }
            }
        }
    }

    fun onUpdateProfile(newUsername: String, newBio: String, newProfilePictureUri: String?) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            val currentUser = authRepository.getUser(email) ?: return@launch
            _uiState.update { it.copy(isLoading = true) }

            val processedPic = if (application != null && !newProfilePictureUri.isNullOrBlank()) {
                MediaStorageManager.encodeToOptimizedBase64(application, newProfilePictureUri, isProfilePicture = true)
            } else newProfilePictureUri

            authRepository.updateUser(currentUser.copy(username = newUsername, bio = newBio, profilePictureUri = processedPic))
            _uiState.update { it.copy(username = newUsername, bio = newBio, profilePictureUri = processedPic, isLoading = false) }
        }
    }

    fun onThemeChange(isDark: Boolean) {
        viewModelScope.launch { preferencesRepository.setTheme(isDark) }
    }

    fun onLocationVisibilityChange(visible: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLocationVisible(visible)
            val email = preferencesRepository.userEmail.first()
            if (!visible && !email.isNullOrBlank()) {
                userRepository.deleteUserLocation(email)
            }
        }
    }

    fun onStatusChange(newStatus: UserStatus) {
        _uiState.update { it.copy(userStatus = newStatus) }
    }

    fun onLocalUsersRadiusChange(radiusKm: Int) {
        _uiState.update { it.copy(localUsersRadiusKm = radiusKm) }
    }

    fun onSearchNearbyUsers(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            if (!_uiState.value.isLocationVisible) {
                userRepository.deleteUserLocation(email)
                return@launch
            }
            _uiState.update { it.copy(isLocalUsersLoading = true) }
            try {
                userRepository.updateUserLocation(email, latitude, longitude)
                val users = userRepository.findNearbyUsers(email, latitude, longitude, _uiState.value.localUsersRadiusKm)
                _uiState.update { it.copy(localUsers = users, isLocalUsersLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLocalUsersLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onClearLocalUsers() {
        _uiState.update { it.copy(localUsers = emptyList(), errorMessage = null, isLocalUsersLoading = false) }
    }

    fun onDeleteAccount(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = preferencesRepository.userEmail.first()
            if (!email.isNullOrBlank()) {
                userRepository.deleteUserLocation(email)
            }
            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                userRepository.clearLocalData()
                preferencesRepository.clearSession()
            }
            _uiState.update { it.copy(isLoading = false) }
            onResult(result)
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first()
            if (!email.isNullOrBlank()) {
                userRepository.deleteUserLocation(email)
            }
            userRepository.clearLocalData()
            preferencesRepository.clearSession()
            authRepository.logout()
        }
    }
}