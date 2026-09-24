package com.example.checkpoint.data.repository

import android.util.Log
import com.example.checkpoint.data.local.dao.CommentDao
import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.PostLikeEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity
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
 * Gestione dei post, tag, commenti, interazioni di like e sincronizzazione remota.
 */
interface PostRepository {

    // ------------------------------------------------------------------------------------------------
    // Lettura e Ricerca Post e Tag
    // ------------------------------------------------------------------------------------------------

    /** Restituisce il flusso di tutti i post con i dati degli autori e lo stato dei like. */
    fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>>

    /** Cerca un post tramite il suo ID. */
    suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor?

    /** Restituisce tutti i tag univoci salvati nel DB. */
    fun getAllUniqueTags(): Flow<List<String>>

    /** Restituisce i tag più popolari. */
    fun getTrendingTags(): Flow<List<String>>

    /** Cerca i post filtrando per titolo, autore o messaggio. */
    fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>

    /** Cerca i post associati a un determinato tag. */
    fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>

    /** Cerca i tag univoci corrispondenti al testo di ricerca. */
    fun searchUniqueTags(query: String): Flow<List<String>>


    // ------------------------------------------------------------------------------------------------
    // Creazione, Eliminazione e Like
    // ------------------------------------------------------------------------------------------------

    /** Crea un nuovo post su Firestore e lo salva nella cache locale. */
    suspend fun createPost(post: PostEntity, tags: List<String>)

    /** Elimina un post sia da Firestore sia dal DB locale. */
    suspend fun deletePost(postId: String)

    /** Alterna lo stato del like di un utente per un determinato post. */
    suspend fun toggleLike(postId: String, userEmail: String)


    // ------------------------------------------------------------------------------------------------
    // Gestione Commenti
    // ------------------------------------------------------------------------------------------------

    /** Restituisce il flusso dei commenti per un post. */
    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>>

    /** Aggiunge un commento a un post salvandolo in locale e su Firestore. */
    suspend fun addComment(comment: CommentEntity)

    /** Restituisce il conteggio dei commenti per un post. */
    fun getCommentCount(postId: String): Flow<Int>


    // ------------------------------------------------------------------------------------------------
    // Sincronizzazione Remota
    // ------------------------------------------------------------------------------------------------

    /** Sincronizza i post, i tag, i like e i commenti da Firestore salvandoli in Room. */
    suspend fun syncPostsLikesAndComments()

    /** Avvia l'ascolto in tempo reale dei post, commenti e like da Firestore. */
    fun startRealtimeSync()

    /** Interrompe gli ascoltatori in tempo reale. */
    fun stopRealtimeSync()
}

/**
 * Implementazione concreta di [PostRepository] integrata con Room e Cloud Firestore.
 */
class PostRepositoryImpl(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val userDao: UserDao,
    private val notificationRepository: NotificationRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PostRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var postsListener: ListenerRegistration? = null
    private var likesListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null


    // ------------------------------------------------------------------------------------------------
    // Lettura e Ricerca Post e Tag
    // ------------------------------------------------------------------------------------------------

    override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> =
        postDao.getAllPostsWithAuthor(currentUserEmail)

    override suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor? =
        postDao.getPostById(postId, currentUserEmail)

    override fun getAllUniqueTags(): Flow<List<String>> =
        postDao.searchUniqueTags("")

    override fun getTrendingTags(): Flow<List<String>> =
        postDao.getTrendingTags()

    override fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> =
        postDao.searchPostsByTitle(query, currentUserEmail)

    override fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> =
        postDao.searchPostsByTag(query, currentUserEmail)

    override fun searchUniqueTags(query: String): Flow<List<String>> =
        postDao.searchUniqueTags(query)


    // ------------------------------------------------------------------------------------------------
    // Creazione, Eliminazione e Like
    // ------------------------------------------------------------------------------------------------

    override suspend fun createPost(post: PostEntity, tags: List<String>) {
        try {
            val postToUpload = if (post.id.isBlank()) post.copy(id = UUID.randomUUID().toString()) else post
            val postData = hashMapOf(
                "id" to postToUpload.id,
                "authorEmail" to postToUpload.authorEmail,
                "title" to postToUpload.title,
                "initialMessage" to postToUpload.initialMessage,
                "timestamp" to postToUpload.timestamp,
                "mediaUri" to postToUpload.mediaUri,
                "mediaType" to postToUpload.mediaType.name,
                "tags" to tags,
            )
            firestore.collection("posts").document(postToUpload.id).set(postData).await()
            postDao.insertPostWithTags(postToUpload, tags.map { PostTagEntity(postToUpload.id, it) })
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Errore createPost: ${e.message}")
        }
    }

    override suspend fun deletePost(postId: String) {
        try {
            firestore.collection("posts").document(postId).delete().await()
            postDao.deletePost(postId)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Errore deletePost: ${e.message}")
        }
    }

    override suspend fun toggleLike(postId: String, userEmail: String) {
        try {
            val likeId = "${postId}_$userEmail"
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
                        recipientEmail = recipientEmail,
                        title = "Nuovo like",
                        body = "$userEmail ha messo like",
                        route = "post_detail",
                        type = "like",
                        senderEmail = userEmail,
                        targetId = postId,
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Errore toggleLike: ${e.message}")
        }
    }


    // ------------------------------------------------------------------------------------------------
    // Gestione Commenti
    // ------------------------------------------------------------------------------------------------

    override fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> =
        commentDao.getCommentsForPost(postId)

    override fun getCommentCount(postId: String): Flow<Int> =
        commentDao.getCommentCount(postId)

    override suspend fun addComment(comment: CommentEntity) {
        try {
            firestore.collection("comments").document(comment.id).set(comment).await()
            commentDao.insertComment(comment)
            val recipientEmail = firestore.collection("posts").document(comment.postId).get().await().getString("authorEmail")
            if (!recipientEmail.isNullOrBlank() && recipientEmail != comment.authorEmail) {
                notificationRepository.createNotification(
                    recipientEmail = recipientEmail,
                    title = "Nuovo commento",
                    body = "${comment.authorEmail} ha commentato",
                    route = "post_detail",
                    type = "comment",
                    senderEmail = comment.authorEmail,
                    targetId = comment.postId,
                )
            }
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Errore addComment: ${e.message}")
        }
    }


    // ------------------------------------------------------------------------------------------------
    // Sincronizzazione Remota
    // ------------------------------------------------------------------------------------------------

    override suspend fun syncPostsLikesAndComments() {
        try {
            val postsSnapshot = firestore.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val remotePosts = postsSnapshot.toObjects(PostEntity::class.java)
            val remoteIds = remotePosts.map { it.id }

            if (remoteIds.isNotEmpty()) postDao.deletePostsNotIn(remoteIds)
            postDao.insertPosts(remotePosts)

            postsSnapshot.documents.forEach { doc ->
                val postId = doc.id
                @Suppress("UNCHECKED_CAST")
                val tags = doc.get("tags") as? List<String>
                if (!tags.isNullOrEmpty()) {
                    postDao.insertTags(tags.map { PostTagEntity(postId, it) })
                }
            }

            val authorEmails = remotePosts.map { it.authorEmail }.distinct()
            authorEmails.chunked(10).forEach { chunk ->
                if (chunk.isNotEmpty()) {
                    val usersSnap = firestore.collection("users").whereIn("email", chunk).get().await()
                    userDao.insertUsers(usersSnap.toObjects(UserEntity::class.java))
                }
            }

            if (remoteIds.isEmpty()) return

            remoteIds.chunked(10).forEach { chunk ->
                if (chunk.isNotEmpty()) {
                    val likesSnapshot = firestore.collection("likes").whereIn("postId", chunk).get().await()
                    likesSnapshot.documents.forEach { doc ->
                        val postId = doc.getString("postId")
                        val userEmail = doc.getString("userEmail")
                        if (postId != null && userEmail != null) postDao.insertLike(PostLikeEntity(postId, userEmail))
                    }

                    val commentsSnapshot = firestore.collection("comments").whereIn("postId", chunk).get().await()
                    val comments = commentsSnapshot.toObjects(CommentEntity::class.java)
                    commentDao.insertComments(comments)

                    val commentAuthors = comments.map { it.authorEmail }.distinct()
                    commentAuthors.chunked(10).forEach { authorChunk ->
                        if (authorChunk.isNotEmpty()) {
                            val commentUsersSnap = firestore.collection("users").whereIn("email", authorChunk).get().await()
                            userDao.insertUsers(commentUsersSnap.toObjects(UserEntity::class.java))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Sync Posts fail: ${e.message}")
        }
    }

    override fun startRealtimeSync() {
        stopRealtimeSync()
        postsListener = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                snapshot?.documentChanges?.forEach { change ->
                    val post = change.document.toObject(PostEntity::class.java)
                    @Suppress("UNCHECKED_CAST")
                    val tags = change.document.get("tags") as? List<String>
                    repositoryScope.launch {
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            postDao.insertPost(post)
                            if (!tags.isNullOrEmpty()) {
                                postDao.insertTags(tags.map { PostTagEntity(post.id, it) })
                            }
                            if (userDao.getUserByEmail(post.authorEmail) == null) {
                                try {
                                    val userDoc = firestore.collection("users").document(post.authorEmail).get().await()
                                    userDoc.toObject(UserEntity::class.java)?.let { userDao.insertUser(it) }
                                } catch (e: Exception) {
                                    Log.e("CheckPointDebug", "Author sync fail: ${e.message}")
                                }
                            }
                        } else if (change.type == DocumentChange.Type.REMOVED) {
                            postDao.deletePost(change.document.id)
                        }
                    }
                }
            }

        commentsListener = firestore.collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                snapshot?.toObjects(CommentEntity::class.java)?.let { comments ->
                    repositoryScope.launch {
                        commentDao.insertComments(comments)
                        comments.forEach { comment ->
                            if (userDao.getUserByEmail(comment.authorEmail) == null) {
                                try {
                                    val userDoc = firestore.collection("users").document(comment.authorEmail).get().await()
                                    userDoc.toObject(UserEntity::class.java)?.let { userDao.insertUser(it) }
                                } catch (e: Exception) {
                                    Log.e("CheckPointDebug", "Comment author sync fail: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }

        likesListener = firestore.collection("likes").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            snapshot?.documentChanges?.forEach { change ->
                val postId = change.document.getString("postId")
                val userEmail = change.document.getString("userEmail")
                if (postId != null && userEmail != null) {
                    repositoryScope.launch {
                        if (change.type == DocumentChange.Type.ADDED) {
                            postDao.insertLike(PostLikeEntity(postId, userEmail))
                        } else if (change.type == DocumentChange.Type.REMOVED) {
                            postDao.deleteLike(postId, userEmail)
                        }
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
