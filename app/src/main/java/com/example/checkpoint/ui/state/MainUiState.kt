package com.example.checkpoint.ui.state

import androidx.compose.ui.graphics.Color
import com.example.checkpoint.data.local.entity.UserEntity

// ------------------------------------------------------------------------------------------------
// Enumerazioni di Stato
// ------------------------------------------------------------------------------------------------

/** Stato di presenza dell'utente (Online, Occupato, Offline). */
enum class UserStatus(val color: Color, val label: String) {
    Online(Color.Green, "Online"),
    Busy(Color.Red, "Busy"),
    Offline(Color.Gray, "Offline")
}

/** Ordine di ordinamento del feed dei post. */
enum class PostSortOrder {
    NEWEST, TRENDING
}


// ------------------------------------------------------------------------------------------------
// Main UiState
// ------------------------------------------------------------------------------------------------

/** Stato globale dell'interfaccia utente condiviso nella finestra principale dell'app. */
data class MainUiState(
    val currentUserEmail: String? = null,
    val isDarkTheme: Boolean = true,
    val isRefreshing: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val selectedUserForPopup: UserEntity? = null,
    val isFollowingSelectedUser: Boolean = false,
    val selectedUserFollowersCount: Int = 0,
    val selectedUserFollowingCount: Int = 0,
    val errorMessage: String? = null,
)


// ------------------------------------------------------------------------------------------------
// Notifiche di Sistema
// ------------------------------------------------------------------------------------------------

/** Modello di notifica dell'applicazione. */
data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val route: String = "home",
    val time: String = "",
    val type: String = "general",
    val senderEmail: String? = null,
    val targetId: String? = null,
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
)
