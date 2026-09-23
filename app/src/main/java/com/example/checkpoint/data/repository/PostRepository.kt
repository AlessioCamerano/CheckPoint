package com.example.checkpoint.data.repository

import android.util.Log
import com.example.checkpoint.data.local.dao.CommentDao
import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.PostLikeEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.model.CommentWithAuthor
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository dedicato al feed e interazioni sui post.
 * Implementa query ottimizzate scaricando like e commenti solo per i post
 * attualmente presenti nel feed, limitando la mole di dati elaborati.
 */
interface PostRepository {
    fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>>
    suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor?
    fun getAllUniqueTags(): Flow<List<String>>
    fun getTrendingTags(): Flow<List<String>>
    fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun searchUniqueTags(query: String): Flow<List<String>>
    suspend fun createPost(post: PostEntity, tags: List<String>)
    suspend fun deletePost(postId: String)
    suspend fun toggleLike(postId: String, userEmail: String)
    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>>
    suspend fun addComment(comment: CommentEntity)
    fun getCommentCount(postId: String): Flow<Int>
    fun getAllCommentsFlow(): Flow<List<CommentWithAuthor>>
    suspend fun syncPostsLikesAndComments()
    fun startRealtimeSync()
    fun stopRealtimeSync()
}

class PostRepositoryImpl(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val notificationRepository: NotificationRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PostRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var postsListener: ListenerRegistration? = null
    private var likesListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null

    override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> = postDao.getAllPostsWithAuthor(currentUserEmail)
    override suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor? = postDao.getPostById(postId, currentUserEmail)
    override fun getAllUniqueTags(): Flow<List<String>> = postDao.searchUniqueTags("")
    override fun getTrendingTags(): Flow<List<String>> = postDao.getTrendingTags()
    override fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> = postDao.searchPostsByTitle(query, currentUserEmail)
    override fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> = postDao.searchPostsByTag(query, currentUserEmail)
    override fun searchUniqueTags(query: String): Flow<List<String>> = postDao.searchUniqueTags(query)
    override fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> = commentDao.getCommentsForPost(postId)
    override fun getCommentCount(postId: String): Flow<Int> = commentDao.getCommentCount(postId)
    override fun getAllCommentsFlow(): Flow<List<CommentWithAuthor>> = commentDao.getAllCommentsFlow()

    override suspend fun createPost(post: PostEntity, tags: List<String>) {
        try {
            val postToUpload = if (post.id.isBlank()) post.copy(id = UUID.randomUUID().toString()) else post
            val postData = hashMapOf(
                "id" to postToUpload.id, "authorEmail" to postToUpload.authorEmail, "title" to postToUpload.title,
                "initialMessage" to postToUpload.initialMessage, "timestamp" to postToUpload.timestamp,
                "mediaUri" to postToUpload.mediaUri, "mediaType" to postToUpload.mediaType.name, "tags" to tags
            )
            firestore.collection("posts").document(postToUpload.id).set(postData).await()
            postDao.insertPostWithTags(postToUpload, tags.map { PostTagEntity(postToUpload.id, it) })
        } catch (e: Exception) { Log.e("CheckPointDebug", "Errore createPost: ${e.message}") }
    }

    override suspend fun deletePost(postId: String) {
        try {
            firestore.collection("posts").document(postId).delete().await()
            postDao.deletePost(postId)
        } catch (e: Exception) { Log.e("CheckPointDebug", "Errore deletePost: ${e.message}") }
    }

    override suspend fun toggleLike(postId: String, userEmail: String) {
        try {
            val likeId = "${postId}_${userEmail}"
            val likeRef = firestore.collection("likes").document(likeId)
            if (likeRef.get().await().exists()) {
                likeRef.delete().await()
                postDao.deleteLike(postId, userEmail)
            } else {
                likeRef.set(mapOf("postId" to postId, "userEmail" to userEmail)).await()
                postDao.insertLike(PostLikeEntity(postId, userEmail))
                val recipientEmail = firestore.collection("posts").document(postId).get().await().getString("authorEmail")
                if (!recipientEmail.isNullOrBlank() && recipientEmail != userEmail) {
                    notificationRepository.createNotification(
                        recipientEmail = recipientEmail, title = "Nuovo like", body = "$userEmail ha messo like",
                        route = "post_detail", type = "like", senderEmail = userEmail, targetId = postId
                    )
                }
            }
        } catch (e: Exception) { Log.e("CheckPointDebug", "Errore toggleLike: ${e.message}") }
    }

    override suspend fun addComment(comment: CommentEntity) {
        try {
            firestore.collection("comments").document(comment.id).set(comment).await()
            commentDao.insertComment(comment)
            val recipientEmail = firestore.collection("posts").document(comment.postId).get().await().getString("authorEmail")
            if (!recipientEmail.isNullOrBlank() && recipientEmail != comment.authorEmail) {
                notificationRepository.createNotification(
                    recipientEmail = recipientEmail, title = "Nuovo commento", body = "${comment.authorEmail} ha commentato",
                    route = "post_detail", type = "comment", senderEmail = comment.authorEmail, targetId = comment.postId
                )
            }
        } catch (e: Exception) { Log.e("CheckPointDebug", "Errore addComment: ${e.message}") }
    }

    override suspend fun syncPostsLikesAndComments() {
        try {
            // Scarica i 50 post più recenti
            val postsSnapshot = firestore.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get().await()
            val remotePosts = postsSnapshot.toObjects(PostEntity::class.java)
            val remoteIds = remotePosts.map { it.id }

            if (remoteIds.isNotEmpty()) postDao.deletePostsNotIn(remoteIds)
            postDao.insertPosts(remotePosts)

            remotePosts.forEach { post ->
                val tags = firestore.collection("posts").document(post.id).get().await().get("tags") as? List<String>
                tags?.let { postDao.insertTags(it.map { tag -> PostTagEntity(post.id, tag) }) }
            }

            if (remoteIds.isEmpty()) return

            // Sincronizza like e commenti solo per i post scaricati, partizionando le richieste in blocchi da 10
            remoteIds.chunked(10).forEach { chunk ->
                if (chunk.isNotEmpty()) {
                    val likesSnapshot = firestore.collection("likes").whereIn("postId", chunk).get().await()
                    likesSnapshot.documents.forEach { doc ->
                        val postId = doc.getString("postId")
                        val userEmail = doc.getString("userEmail")
                        if (postId != null && userEmail != null) postDao.insertLike(PostLikeEntity(postId, userEmail))
                    }

                    val commentsSnapshot = firestore.collection("comments").whereIn("postId", chunk).get().await()
                    commentDao.insertComments(commentsSnapshot.toObjects(CommentEntity::class.java))
                }
            }
        } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Posts fail: ${e.message}") }
    }

    override fun startRealtimeSync() {
        stopRealtimeSync()
        postsListener = firestore.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                snapshot?.documentChanges?.forEach { change ->
                    val post = change.document.toObject(PostEntity::class.java)
                    repositoryScope.launch {
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) postDao.insertPost(post)
                        else if (change.type == DocumentChange.Type.REMOVED) postDao.deletePost(change.document.id)
                    }
                }
            }

        commentsListener = firestore.collection("comments").orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                snapshot?.toObjects(CommentEntity::class.java)?.let { repositoryScope.launch { commentDao.insertComments(it) } }
            }

        likesListener = firestore.collection("likes").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            snapshot?.documentChanges?.forEach { change ->
                val postId = change.document.getString("postId")
                val userEmail = change.document.getString("userEmail")
                if (postId != null && userEmail != null) {
                    repositoryScope.launch {
                        if (change.type == DocumentChange.Type.ADDED) postDao.insertLike(PostLikeEntity(postId, userEmail))
                        else if (change.type == DocumentChange.Type.REMOVED) postDao.deleteLike(postId, userEmail)
                    }
                }
            }
        }
    }

    override fun stopRealtimeSync() {
        postsListener?.remove()
        commentsListener?.remove()
        likesListener?.remove()
    }
}