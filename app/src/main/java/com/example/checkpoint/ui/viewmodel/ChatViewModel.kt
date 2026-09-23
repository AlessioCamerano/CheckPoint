package com.example.checkpoint.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.repository.*
import com.example.checkpoint.ui.state.ChatUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var chatJob: Job? = null

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getDatabase(app)
                return ChatViewModel(
                    chatRepository = ChatRepositoryImpl(db.chatDao(), NotificationRepositoryImpl()),
                    authRepository = AuthRepositoryImpl(db.userDao()),
                    preferencesRepository = UserPreferencesRepositoryImpl(app)
                ) as T
            }
        }
    }

    init { observeChatPartners() }

    private fun observeChatPartners() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email != null) chatRepository.getChatPartners(email).collectLatest { p -> _uiState.update { it.copy(chatPartners = p) } }
            }
        }
    }

    fun onOpenChat(partner: UserEntity) {
        _uiState.update { it.copy(selectedChatPartner = partner) }
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            val currentEmail = preferencesRepository.userEmail.first() ?: return@launch
            chatRepository.getChatMessages(currentEmail, partner.email).collectLatest { messages ->
                _uiState.update { it.copy(currentChatMessages = messages) }
            }
        }
    }

    fun onOpenChatByEmail(partnerEmail: String) {
        if (partnerEmail.isBlank()) return
        viewModelScope.launch {
            val partner = _uiState.value.chatPartners.firstOrNull { it.email == partnerEmail }
                ?: authRepository.getUser(partnerEmail)
                ?: UserEntity(email = partnerEmail, username = partnerEmail.split("@")[0])
            onOpenChat(partner)
        }
    }

    fun onSendMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val currentEmail = preferencesRepository.userEmail.first() ?: return@launch
            val partnerEmail = _uiState.value.selectedChatPartner?.email ?: return@launch
            chatRepository.sendMessage(MessageEntity(senderEmail = currentEmail, receiverEmail = partnerEmail, content = content))
        }
    }

    fun onCloseChat() {
        chatJob?.cancel()
        _uiState.update { it.copy(selectedChatPartner = null, currentChatMessages = emptyList()) }
    }
}