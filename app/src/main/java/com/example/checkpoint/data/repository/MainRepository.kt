package com.example.checkpoint.data.repository

import android.util.Log
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.dao.ChatDao
import com.example.checkpoint.data.local.dao.CommentDao
import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.FollowEntity
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.PostLikeEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.CommentWithAuthor
import com.example.checkpoint.data.local.model.GeoUser
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.state.AppNotification
import com.example.checkpoint.util.TimeUtils
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query as FirestoreQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository per la gestione dei post e dei dati principali su Firestore e Room.
 */
interface MainRepository {
    fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>>
    suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor?
    fun getAllUniqueTags(): Flow<List<String>>
    fun getTrendingTags(): Flow<List<String>>
    fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun searchUsersByUsername(query: String): Flow<List<UserEntity>>
    fun searchUniqueTags(query: String): Flow<List<String>>
    suspend fun createPost(post: PostEntity, tags: List<String>)
    suspend fun deletePost(postId: String)
    suspend fun toggleLike(postId: String, userEmail: String)
    
    // Sincronizzazione manuale
    suspend fun syncDataFromFirestore(currentUserEmail: String)
    suspend fun syncHighPriorityData(currentUserEmail: String)
    suspend fun clearLocalData()
    
    // operazioni per follow
    fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean>
    suspend fun toggleFollow(followerEmail: String, followedEmail: String)
    fun getFollowersCount(userEmail: String): Flow<Int>
    fun getFollowingCount(userEmail: String): Flow<Int>
    fun getFollowers(userEmail: String): Flow<List<UserEntity>>
    fun getFollowing(userEmail: String): Flow<List<UserEntity>>
    suspend fun updateUserLocation(userEmail: String, latitude: Double, longitude: Double)
    suspend fun deleteUserLocation(userEmail: String)
    suspend fun findNearbyUsers(currentUserEmail: String, latitude: Double, longitude: Double, radiusKm: Int): List<GeoUser>

    // Operazioni per chat
    fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>>
    fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>>
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>
    fun getAllCommentsFlow(): Flow<List<CommentWithAuthor>>
    suspend fun sendMessage(message: MessageEntity)
    suspend fun saveFcmToken(userEmail: String, token: String)
    fun startRealtimeSync(currentUserEmail: String)
    fun stopRealtimeSync()

    // Operazioni per commenti
    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>>
    suspend fun addComment(comment: CommentEntity)
    fun getCommentCount(postId: String): Flow<Int>

    // Operazioni per notifiche
    fun observeNotifications(currentUserEmail: String): Flow<List<AppNotification>>
    suspend fun createNotification(
        recipientEmail: String,
        title: String,
        body: String,
        route: String = "home",
        type: String = "general",
        senderEmail: String? = null,
        targetId: String? = null
    )
    suspend fun deleteNotification(notificationId: String)
}

class MainRepositoryImpl(
    private val postDao: PostDao, 
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val commentDao: CommentDao,
    private val database: AppDatabase, // Passiamo il DB per clearAllTables
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MainRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var messagesListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null
    private var likesListener: ListenerRegistration? = null
    private var followsListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null

    override suspend fun saveFcmToken(userEmail: String, token: String) {
        if (userEmail.isBlank() || token.isBlank()) return
        firestore.collection("users")
            .document(userEmail)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    override suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor? {
        return postDao.getPostById(postId, currentUserEmail)
    }

    override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> {
        return postDao.getAllPostsWithAuthor(currentUserEmail)
    }

    override fun getAllUniqueTags(): Flow<List<String>> {
        return postDao.searchUniqueTags("")
    }

    override fun getTrendingTags(): Flow<List<String>> {
        return postDao.getTrendingTags()
    }

    override fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> {
        return postDao.searchPostsByTitle(query, currentUserEmail)
    }

    override fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> {
        return postDao.searchPostsByTag(query, currentUserEmail)
    }

    override fun searchUsersByUsername(query: String): Flow<List<UserEntity>> {
        return userDao.searchUsersByUsername(query)
    }

    override fun searchUniqueTags(query: String): Flow<List<String>> {
        return postDao.searchUniqueTags(query)
    }

    override suspend fun syncDataFromFirestore(currentUserEmail: String) {
        try {
            Log.d("CheckPointDebug", "Main: INIZIO SINCRONIZZAZIONE GLOBALE...")

            // 1. UTENTI
            try {
                val usersSnapshot = firestore.collection("users").get().await()
                val remoteUsers = usersSnapshot.toObjects(UserEntity::class.java)
                userDao.insertUsers(remoteUsers)
                Log.d("CheckPointDebug", "Main: Sincronizzati ${remoteUsers.size} utenti")
            } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Users fail: ${e.message}") }

            // 2. POST & TAGS
            try {
                val postsSnapshot = firestore.collection("posts")
                    .orderBy("timestamp", FirestoreQuery.Direction.DESCENDING)
                    .limit(50).get().await()
                val remotePosts = postsSnapshot.toObjects(PostEntity::class.java)
                postDao.insertPosts(remotePosts)
                Log.d("CheckPointDebug", "Main: Sincronizzati ${remotePosts.size} post")
                
                // Sincronizzati anche i TAG dei post scaricati
                remotePosts.forEach { post ->
                    val tagsSnapshot = firestore.collection("posts").document(post.id).get().await()
                    @Suppress("UNCHECKED_CAST")
                    val tags = tagsSnapshot.get("tags") as? List<String>
                    if (tags != null) {
                        val tagEntities = tags.map { PostTagEntity(post.id, it) }
                        postDao.insertTags(tagEntities)
                    }
                }
            } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Posts/Tags fail: ${e.message}") }

            // 3. LIKES
            try {
                val likesSnapshot = firestore.collection("likes").get().await()
                likesSnapshot.documents.forEach { doc ->
                    val postId = doc.getString("postId")
                    val userEmail = doc.getString("userEmail")
                    if (postId != null && userEmail != null) {
                        postDao.insertLike(PostLikeEntity(postId, userEmail))
                    }
                }
                Log.d("CheckPointDebug", "Main: Sincronizzati ${likesSnapshot.size()} likes")
            } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Likes fail: ${e.message}") }

            // 4. FOLLOWS
            try {
                val followsSnapshot = firestore.collection("follows").get().await()
                followsSnapshot.documents.forEach { doc ->
                    val follower = doc.getString("followerEmail")
                    val followed = doc.getString("followedEmail")
                    if (follower != null && followed != null) {
                        userDao.insertFollow(FollowEntity(follower, followed))
                    }
                }
                Log.d("CheckPointDebug", "Main: Sincronizzati ${followsSnapshot.size()} follow")
            } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Follows fail: ${e.message}") }

            // 5. MESSAGGI (Sia inviati che ricevuti)
            try {
                // Messaggi ricevuti
                val receivedSnapshot = firestore.collection("messages")
                    .whereIn("receiverEmail", listOf(currentUserEmail, "global"))
                    .get().await()
                
                // Messaggi inviati
                val sentSnapshot = firestore.collection("messages")
                    .whereEqualTo("senderEmail", currentUserEmail)
                    .get().await()

                val allMessages = (receivedSnapshot.toObjects(MessageEntity::class.java) + 
                                  sentSnapshot.toObjects(MessageEntity::class.java))
                
                chatDao.insertMessages(allMessages)
                Log.d("CheckPointDebug", "Main: Sincronizzati ${allMessages.size} messaggi totali")
            } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Messages fail: ${e.message}") }

            // 6. COMMENTI
            try {
                val commentsSnapshot = firestore.collection("comments").limit(100).get().await()
                val remoteComments = commentsSnapshot.toObjects(CommentEntity::class.java)
                commentDao.insertComments(remoteComments)
                Log.d("CheckPointDebug", "Main: Sincronizzati ${remoteComments.size} commenti")
            } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Comments fail: ${e.message}") }

            Log.d("CheckPointDebug", "Main: SINCRONIZZAZIONE COMPLETATA")
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Main: ERRORE FATALE sincronizzazione: ${e.message}", e)
        }
    }

    override suspend fun createPost(post: PostEntity, tags: List<String>) {
        Log.d("CheckPointDebug", "1. Tentativo di creazione post avviato: ${post.title}")
        try {
            val postToUpload = if (post.id.isBlank()) post.copy(id = UUID.randomUUID().toString()) else post
            
            // oggetto mappa per includere i TAG in Firestore
            val postData = hashMapOf(
                "id" to postToUpload.id,
                "authorEmail" to postToUpload.authorEmail,
                "title" to postToUpload.title,
                "initialMessage" to postToUpload.initialMessage,
                "timestamp" to postToUpload.timestamp,
                "mediaUri" to postToUpload.mediaUri,
                "mediaType" to postToUpload.mediaType,
                "tags" to tags
            )
            
            firestore.collection("posts").document(postToUpload.id).set(postData).await()
            Log.d("CheckPointDebug", "2. Post salvato con successo su FIRESTORE")

            val tagEntities = tags.map { PostTagEntity(postToUpload.id, it) }
            postDao.insertPostWithTags(postToUpload, tagEntities)
            Log.d("CheckPointDebug", "3. Post salvato con successo in ROOM locale")
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "ERRORE durante createPost: ${e.message}", e)
        }
    }

    override suspend fun deletePost(postId: String) {
        Log.d("CheckPointDebug", "Main: Eliminazione post $postId")
        try {
            // Elimina da Firestore
            firestore.collection("posts").document(postId).delete().await()
            postDao.deletePost(postId)
            Log.d("CheckPointDebug", "Main: Post $postId eliminato con successo")
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error deleting post: ${e.message}")
        }
    }

    override suspend fun toggleLike(postId: String, userEmail: String) {
        Log.d("CheckPointDebug", "Main: Toggle LIKE su FIRESTORE per post: $postId")
        try {
            val likeId = "${postId}_${userEmail}"
            val likeRef = firestore.collection("likes").document(likeId)
            val doc = likeRef.get().await()
            
            if (doc.exists()) {
                likeRef.delete().await()
                postDao.deleteLike(postId, userEmail)
                Log.d("CheckPointDebug", "Main: LIKE rimosso da FIRESTORE")
            } else {
                likeRef.set(mapOf("postId" to postId, "userEmail" to userEmail)).await()
                postDao.insertLike(PostLikeEntity(postId, userEmail))

                val postDoc = firestore.collection("posts").document(postId).get().await()
                val recipientEmail = postDoc.getString("authorEmail")
                if (!recipientEmail.isNullOrBlank() && recipientEmail != userEmail) {
                    createNotificationInternal(
                        recipientEmail = recipientEmail,
                        title = "Nuovo like",
                        body = "$userEmail ha messo like al tuo post",
                        route = "post_detail",
                        type = "like",
                        senderEmail = userEmail,
                        targetId = postId
                    )
                }

                Log.d("CheckPointDebug", "Main: LIKE aggiunto su FIRESTORE")
            }
            syncDataFromFirestore(userEmail)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Main: ERRORE toggle LIKE FIRESTORE: ${e.message}", e)
        }
    }

    override fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean> {
        return userDao.isFollowing(followerEmail, followedEmail)
    }

    override suspend fun toggleFollow(followerEmail: String, followedEmail: String) {
        Log.d("CheckPointDebug", "Main: Toggle FOLLOW su FIRESTORE da $followerEmail a $followedEmail")
        try {
            val followId = "${followerEmail}_${followedEmail}"
            val followRef = firestore.collection("follows").document(followId)
            val doc = followRef.get().await()
            
            if (doc.exists()) {
                followRef.delete().await()
                userDao.deleteFollow(followerEmail, followedEmail)
                Log.d("CheckPointDebug", "Main: UNFOLLOW eseguito su FIRESTORE")
            } else {
                followRef.set(mapOf("followerEmail" to followerEmail, "followedEmail" to followedEmail)).await()
                userDao.insertFollow(FollowEntity(followerEmail, followedEmail))

                if (followedEmail.isNotBlank() && followedEmail != followerEmail) {
                    createNotificationInternal(
                        recipientEmail = followedEmail,
                        title = "Nuovo follow",
                        body = "$followerEmail ti segue",
                        route = "home",
                        type = "follow",
                        senderEmail = followerEmail,
                        targetId = followedEmail
                    )
                }

                Log.d("CheckPointDebug", "Main: FOLLOW eseguito su FIRESTORE")
            }
            syncDataFromFirestore(followerEmail)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Main: ERRORE toggle FOLLOW FIRESTORE: ${e.message}", e)
        }
    }

    override fun getFollowersCount(userEmail: String): Flow<Int> {
        return userDao.getFollowersCount(userEmail)
    }

    override fun getFollowingCount(userEmail: String): Flow<Int> {
        return userDao.getFollowingCount(userEmail)
    }

    override fun getFollowers(userEmail: String): Flow<List<UserEntity>> {
        return userDao.getFollowers(userEmail)
    }

    override fun getFollowing(userEmail: String): Flow<List<UserEntity>> {
        return userDao.getFollowing(userEmail)
    }

    override suspend fun updateUserLocation(userEmail: String, latitude: Double, longitude: Double) {
        val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude))
        firestore.collection("user_locations")
            .document(userEmail)
            .set(
                mapOf(
                    "userEmail" to userEmail,
                    "lat" to latitude,
                    "lng" to longitude,
                    "geohash" to hash
                )
            )
            .await()
    }

    override suspend fun deleteUserLocation(userEmail: String) {
        Log.d("CheckPointDebug", "Main: Eliminazione posizione utente $userEmail")
        try {
            firestore.collection("user_locations").document(userEmail).delete().await()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error deleting user location: ${e.message}")
        }
    }

    override suspend fun findNearbyUsers(
        currentUserEmail: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): List<GeoUser> {
        val center = GeoLocation(latitude, longitude)
        val radiusM = radiusKm * 1000.0
        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusM)
        val result = mutableMapOf<String, GeoUser>()

        for (b in bounds) {
            val snap = firestore.collection("user_locations")
                .orderBy("geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)
                .get()
                .await()

            for (doc in snap.documents) {
                val email = doc.getString("userEmail") ?: continue
                if (email == currentUserEmail) continue
                val lat = doc.getDouble("lat") ?: continue
                val lng = doc.getDouble("lng") ?: continue
                val distanceM = GeoFireUtils.getDistanceBetween(GeoLocation(lat, lng), center)
                if (distanceM > radiusM) continue

                val user = userDao.getUserByEmail(email) ?: continue
                result[email] = GeoUser(
                    user = user,
                    distanceKm = distanceM / 1000.0
                )
            }
        }

        return result.values.sortedBy { it.distanceKm }
    }

    override fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>> {
        return chatDao.getChatPartners(currentUserEmail)
    }

    override fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>> {
        return chatDao.getChatMessages(user1, user2)
    }

    override fun getAllMessagesFlow(): Flow<List<MessageEntity>> {
        return chatDao.getAllMessagesFlow()
    }

    override fun getAllCommentsFlow(): Flow<List<CommentWithAuthor>> {
        return commentDao.getAllCommentsFlow()
    }

    override suspend fun sendMessage(message: MessageEntity) {
        Log.d("CheckPointDebug", "Main: Invio MESSAGGIO su FIRESTORE da ${message.senderEmail}")
        try {
            firestore.collection("messages").document(message.id).set(message).await()
            Log.d("CheckPointDebug", "Main: Messaggio inviato con successo su FIRESTORE")
            chatDao.insertMessage(message)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Main: ERRORE invio messaggio FIRESTORE: ${e.message}", e)
        }
    }

    override suspend fun syncHighPriorityData(currentUserEmail: String) {
        Log.d("CheckPointDebug", "Main: SINCRONIZZAZIONE ALTA PRIORITÀ (Messaggi & Follow)")
        
        // 1. FOLLOWS
        try {
            val followsSnapshot = firestore.collection("follows").get().await()
            followsSnapshot.documents.forEach { doc ->
                val follower = doc.getString("followerEmail")
                val followed = doc.getString("followedEmail")
                if (follower != null && followed != null) {
                    if (follower == currentUserEmail || followed == currentUserEmail) {
                        userDao.insertFollow(FollowEntity(follower, followed))
                    }
                }
            }
        } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Follows fail: ${e.message}") }

        // 2. MESSAGGI
        try {
            val receivedSnapshot = firestore.collection("messages")
                .whereIn("receiverEmail", listOf(currentUserEmail, "global"))
                .get().await()
            val sentSnapshot = firestore.collection("messages")
                .whereEqualTo("senderEmail", currentUserEmail)
                .get().await()

            val allMessages = (receivedSnapshot.toObjects(MessageEntity::class.java) + 
                              sentSnapshot.toObjects(MessageEntity::class.java))
            chatDao.insertMessages(allMessages)
        } catch (e: Exception) { Log.e("CheckPointDebug", "Sync Messages fail: ${e.message}") }
    }

    override suspend fun clearLocalData() {
        Log.d("CheckPointDebug", "Main: Pulizia DATABASE LOCALE (Mutua Esclusione)")
        try {
            database.clearAllTables()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error clearing local DB: ${e.message}")
        }
    }

    override fun startRealtimeSync(currentUserEmail: String) {
        stopRealtimeSync()

        messagesListener = firestore.collection("messages")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                snapshot?.documentChanges?.forEach { change ->
                    val msg = change.document.toObject(MessageEntity::class.java)
                    if (msg.receiverEmail == currentUserEmail || msg.senderEmail == currentUserEmail || msg.receiverEmail == "global") {
                        repositoryScope.launch {
                            when (change.type) {
                                DocumentChange.Type.ADDED -> chatDao.insertMessage(msg)
                                else -> {}
                            }
                        }
                    }
                }
            }

        // Listener per nuovi post (ultimi 20)
        postsListener = firestore.collection("posts")
            .orderBy("timestamp", FirestoreQuery.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val posts = snapshot?.toObjects(PostEntity::class.java) ?: emptyList()
                if (posts.isNotEmpty()) {
                    repositoryScope.launch { postDao.insertPosts(posts) }
                }
            }

        // Listener per commenti recenti
        commentsListener = firestore.collection("comments")
            .orderBy("timestamp", FirestoreQuery.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val comments = snapshot?.toObjects(CommentEntity::class.java) ?: emptyList()
                if (comments.isNotEmpty()) {
                    repositoryScope.launch { commentDao.insertComments(comments) }
                }
            }

        // Listener per Like
        likesListener = firestore.collection("likes")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                snapshot?.documentChanges?.forEach { change ->
                    val postId = change.document.getString("postId")
                    val userEmail = change.document.getString("userEmail")
                    if (postId != null && userEmail != null) {
                        repositoryScope.launch {
                            when (change.type) {
                                DocumentChange.Type.ADDED ->
                                    postDao.insertLike(PostLikeEntity(postId, userEmail))
                                DocumentChange.Type.REMOVED ->
                                    postDao.deleteLike(postId, userEmail)
                                else -> {}
                            }
                        }
                    }
                }
            }

        // Listener per Follow
        followsListener = firestore.collection("follows")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                snapshot?.documentChanges?.forEach { change ->
                    val follower = change.document.getString("followerEmail")
                    val followed = change.document.getString("followedEmail")
                    if (follower != null && followed != null) {
                        if (follower == currentUserEmail || followed == currentUserEmail) {
                            repositoryScope.launch {
                                when (change.type) {
                                    DocumentChange.Type.ADDED ->
                                        userDao.insertFollow(FollowEntity(follower, followed))
                                    DocumentChange.Type.REMOVED ->
                                        userDao.deleteFollow(follower, followed)
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
    }

    override fun stopRealtimeSync() {
        messagesListener?.remove()
        postsListener?.remove()
        commentsListener?.remove()
        likesListener?.remove()
        followsListener?.remove()
        notificationsListener?.remove()
    }

    override fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> {
        return commentDao.getCommentsForPost(postId)
    }

    override suspend fun addComment(comment: CommentEntity) {
        Log.d("CheckPointDebug", "Main: Aggiunta COMMENTO su FIRESTORE per post ${comment.postId}")
        try {
            firestore.collection("comments").document(comment.id).set(comment).await()
            Log.d("CheckPointDebug", "Main: Commento aggiunto con successo su FIRESTORE")
            commentDao.insertComment(comment)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Main: ERRORE aggiunta commento FIRESTORE: ${e.message}", e)
        }
    }

    override fun getCommentCount(postId: String): Flow<Int> {
        return commentDao.getCommentCount(postId)
    }

    private suspend fun createNotificationInternal(
        recipientEmail: String,
        title: String,
        body: String,
        route: String = "home",
        type: String = "general",
        senderEmail: String? = null,
        targetId: String? = null
    ) {
        createNotification(recipientEmail, title, body, route, type, senderEmail, targetId)
    }

    override suspend fun createNotification(
        recipientEmail: String,
        title: String,
        body: String,
        route: String,
        type: String,
        senderEmail: String?,
        targetId: String?
    ) {
        if (recipientEmail.isBlank()) return
        try {
            val docRef = firestore.collection("notifications").document()
            val notificationData = hashMapOf(
                "id" to docRef.id,
                "recipientEmail" to recipientEmail,
                "title" to title,
                "body" to body,
                "route" to route,
                "type" to type,
                "senderEmail" to senderEmail,
                "targetId" to targetId,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )
            docRef.set(notificationData).await()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error creating notification: ${e.message}", e)
        }
    }

    override suspend fun deleteNotification(notificationId: String) {
        if (notificationId.isBlank()) return
        try {
            firestore.collection("notifications").document(notificationId).delete().await()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error deleting notification: ${e.message}", e)
        }
    }

    override fun observeNotifications(currentUserEmail: String): Flow<List<AppNotification>> = callbackFlow {
        if (currentUserEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientEmail", currentUserEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val body = doc.getString("body") ?: ""
                    val route = doc.getString("route") ?: "home"
                    val type = doc.getString("type") ?: "general"
                    val senderEmail = doc.getString("senderEmail")
                    val targetId = doc.getString("targetId")
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val isRead = doc.getBoolean("isRead") ?: false
                    AppNotification(
                        id = id,
                        title = title,
                        body = body,
                        route = route,
                        time = TimeUtils.getTimeAgo(timestamp),
                        type = type,
                        senderEmail = senderEmail,
                        targetId = targetId,
                        timestamp = timestamp,
                        isRead = isRead
                    )
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }
}
