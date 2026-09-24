package com.example.checkpoint.ui.state

import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.CommentWithAuthor
import com.example.checkpoint.data.local.model.GeoUser
import com.example.checkpoint.data.local.model.PostWithAuthor

// ------------------------------------------------------------------------------------------------
// Feed UiState
// ------------------------------------------------------------------------------------------------

/** Stato della UI per il feed dei post e la schermata dettagli. */
data class FeedUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val postSuccess: Boolean = false,
    val errorMessage: String? = null,
    val posts: List<PostWithAuthor> = emptyList(),
    val sortOrder: PostSortOrder = PostSortOrder.NEWEST,
    val selectedPost: PostWithAuthor? = null,
    val currentPostComments: List<CommentWithAuthor> = emptyList(),
)


// ------------------------------------------------------------------------------------------------
// Profile UiState
// ------------------------------------------------------------------------------------------------

/** Stato della UI per la schermata del profilo e delle impostazioni utente. */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val username: String = "Username",
    val bio: String = "",
    val profilePictureUri: String? = null,
    val currentUserEmail: String? = null,
    val isDarkTheme: Boolean = true,
    val isLocationVisible: Boolean = true,
    val userStatus: UserStatus = UserStatus.Online,
    val followersList: List<UserEntity> = emptyList(),
    val followingList: List<UserEntity> = emptyList(),
    val localUsers: List<GeoUser> = emptyList(),
    val localUsersRadiusKm: Int = 25,
    val isLocalUsersLoading: Boolean = false,
    val errorMessage: String? = null,
)


// ------------------------------------------------------------------------------------------------
// Search UiState
// ------------------------------------------------------------------------------------------------

/** Stato della UI per la ricerca di post, utenti e tag. */
data class SearchUiState(
    val searchHistory: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val trendingTags: List<String> = emptyList(),
    val searchResultsUsers: List<UserEntity> = emptyList(),
    val searchResultsPosts: List<PostWithAuthor> = emptyList(),
    val searchResultsTags: List<String> = emptyList(),
    val tagResults: List<PostWithAuthor> = emptyList(),
    val searchSortOrder: PostSortOrder = PostSortOrder.NEWEST,
    val errorMessage: String? = null,
)


// ------------------------------------------------------------------------------------------------
// Chat UiState
// ------------------------------------------------------------------------------------------------

/** Stato della UI per la messaggistica e la conversazione attiva. */
data class ChatUiState(
    val chatPartners: List<UserEntity> = emptyList(),
    val currentChatMessages: List<MessageEntity> = emptyList(),
    val selectedChatPartner: UserEntity? = null,
    val errorMessage: String? = null,
)
