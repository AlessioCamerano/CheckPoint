package com.example.checkpoint.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.data.repository.*
import com.example.checkpoint.ui.state.FeedUiState
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.util.MediaStorageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class FeedViewModel(
    private val application: Application?,
    private val postRepository: PostRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private val _sortOrder = MutableStateFlow(PostSortOrder.NEWEST)
    private var commentJob: Job? = null

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getDatabase(app)
                val notifRepo = NotificationRepositoryImpl()
                return FeedViewModel(
                    application = app,
                    postRepository = PostRepositoryImpl(db.postDao(), db.commentDao(), notifRepo),
                    preferencesRepository = UserPreferencesRepositoryImpl(app)
                ) as T
            }
        }
    }

    init { observePosts() }

    private fun observePosts() {
        viewModelScope.launch {
            combine(preferencesRepository.userEmail, _sortOrder) { email, sortOrder -> email to sortOrder }
                .collectLatest { (email, sortOrder) ->
                    if (email != null) {
                        postRepository.getAllPosts(email).collectLatest { result ->
                            val sortedList = when (sortOrder) {
                                PostSortOrder.NEWEST -> result.sortedByDescending { it.post.timestamp }
                                PostSortOrder.TRENDING -> result.sortedByDescending { it.favoriteCount }
                            }
                            _uiState.update { state ->
                                state.copy(
                                    posts = sortedList,
                                    selectedPost = state.selectedPost?.let { current -> result.firstOrNull { it.post.id == current.post.id } ?: current },
                                    sortOrder = sortOrder,
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onSortOrderChange(newOrder: PostSortOrder) {
        if (_sortOrder.value == newOrder) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(600.milliseconds)
            _sortOrder.value = newOrder
        }
    }

    fun onToggleLike(postId: String) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            postRepository.toggleLike(postId, email)
        }
    }

    fun onCreatePost(title: String, message: String, mediaUri: String?, mediaType: MediaType, tags: List<String>) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val email = preferencesRepository.userEmail.first() ?: return@launch
                val processedMediaUri = if (application != null && !mediaUri.isNullOrBlank()) {
                    MediaStorageManager.encodeToOptimizedBase64(application, mediaUri, isProfilePicture = false)
                } else null
                val resolvedMediaType = if (!processedMediaUri.isNullOrBlank()) MediaType.IMAGE else MediaType.NONE
                
                postRepository.createPost(PostEntity(authorEmail = email, title = title, initialMessage = message, mediaUri = processedMediaUri, mediaType = resolvedMediaType), tags)
                _uiState.update { it.copy(isLoading = false, postSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch { postRepository.deletePost(postId) }
    }

    fun resetPostSuccess() { _uiState.update { it.copy(postSuccess = false) } }

    fun onOpenPostDetail(post: PostWithAuthor) {
        _uiState.update { it.copy(selectedPost = post) }
        commentJob?.cancel()
        commentJob = viewModelScope.launch {
            postRepository.getCommentsForPost(post.post.id).collectLatest { comments ->
                _uiState.update { it.copy(currentPostComments = comments) }
            }
        }
    }

    fun onClosePostDetail() {
        commentJob?.cancel()
        _uiState.update { it.copy(selectedPost = null, currentPostComments = emptyList()) }
    }

    fun onAddComment(content: String, parentCommentId: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            val postId = _uiState.value.selectedPost?.post?.id ?: return@launch
            postRepository.addComment(CommentEntity(postId = postId, authorEmail = email, content = content, parentCommentId = parentCommentId))
        }
    }
}