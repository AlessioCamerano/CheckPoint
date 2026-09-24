package com.example.checkpoint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.CheckPointApplication
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.ChatRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.ui.state.ChatUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel per la gestione delle conversazioni di chat in tempo reale.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var chatJob: Job? = null

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as CheckPointApplication
                return ChatViewModel(
                    chatRepository = application.container.chatRepository,
                    authRepository = application.container.authRepository,
                    preferencesRepository = application.container.preferencesRepository,
                ) as T
            }
        }
    }

    init {
        observeChatPartners()
    }


    // ------------------------------------------------------------------------------------------------
    // Gestione Conversazioni e Messaggi
    // ------------------------------------------------------------------------------------------------

    private fun observeChatPartners() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email != null) {
                    chatRepository.getChatPartners(email).collectLatest { partners ->
                        _uiState.update { it.copy(chatPartners = partners) }
                    }
                }
            }
        }
    }

    /** Apre la chat con un determinato utente partner e ne osserva i messaggi. */
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

    /** Apre la chat cercando l'utente per email. */
    fun onOpenChatByEmail(partnerEmail: String) {
        if (partnerEmail.isBlank()) return
        viewModelScope.launch {
            val partner = _uiState.value.chatPartners.firstOrNull { it.email == partnerEmail }
                ?: authRepository.getUser(partnerEmail)
                ?: UserEntity(email = partnerEmail, username = partnerEmail.split("@")[0])
            onOpenChat(partner)
        }
    }

    /** Invia un nuovo messaggio di testo nella conversazione attiva. */
    fun onSendMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val currentEmail = preferencesRepository.userEmail.first() ?: return@launch
            val partnerEmail = _uiState.value.selectedChatPartner?.email ?: return@launch
            chatRepository.sendMessage(MessageEntity(senderEmail = currentEmail, receiverEmail = partnerEmail, content = content))
        }
    }

    /** Chiude la conversazione attiva. */
    fun onCloseChat() {
        chatJob?.cancel()
        _uiState.update { it.copy(selectedChatPartner = null, currentChatMessages = emptyList()) }
    }
}
