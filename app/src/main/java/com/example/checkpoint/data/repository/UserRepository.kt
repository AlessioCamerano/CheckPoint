package com.example.checkpoint.data.repository

import android.util.Log
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.FollowEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.GeoUser
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Gestione dei dati utente, relazioni di follow, geolocalizzazione e notifiche di sistema.
 */
interface UserRepository {

    // ------------------------------------------------------------------------------------------------
    // Ricerca Utenti e Follow
    // ------------------------------------------------------------------------------------------------

    /** Cerca gli utenti per username. */
    fun searchUsersByUsername(query: String): Flow<List<UserEntity>>

    /** Verifica se un utente ne segue un altro. */
    fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean>

    /** Alterna lo stato di follow/unfollow tra due utenti. */
    suspend fun toggleFollow(followerEmail: String, followedEmail: String)

    /** Conteggio dei follower di un utente. */
    fun getFollowersCount(userEmail: String): Flow<Int>

    /** Conteggio degli utenti seguiti da un utente. */
    fun getFollowingCount(userEmail: String): Flow<Int>

    /** Elenco dei follower di un utente. */
    fun getFollowers(userEmail: String): Flow<List<UserEntity>>

    /** Elenco degli utenti seguiti da un utente. */
    fun getFollowing(userEmail: String): Flow<List<UserEntity>>


    // ------------------------------------------------------------------------------------------------
    // Geolocalizzazione e Notifiche FCM
    // ------------------------------------------------------------------------------------------------

    /** Aggiorna le coordinate geografiche dell'utente e il relativo geohash su Firestore. */
    suspend fun updateUserLocation(userEmail: String, latitude: Double, longitude: Double)

    /** Rimuove la posizione dell'utente da Firestore. */
    suspend fun deleteUserLocation(userEmail: String)

    /** Cerca gli utenti nelle vicinanze in base al raggio specificato in chilometri. */
    suspend fun findNearbyUsers(currentUserEmail: String, latitude: Double, longitude: Double, radiusKm: Int): List<GeoUser>

    /** Salva il token FCM per le notifiche push. */
    suspend fun saveFcmToken(userEmail: String, token: String)


    // ------------------------------------------------------------------------------------------------
    // Sincronizzazione Remota e Pulizia
    // ------------------------------------------------------------------------------------------------

    /** Sincronizza il profilo dell'utente corrente, le sue relazioni di follow e i profili collegati. */
    suspend fun syncUsersAndFollows(currentUserEmail: String)

    /** Elimina tutti i dati salvati nel DB locale SQLite. */
    suspend fun clearLocalData()

    /** Avvia l'ascolto in tempo reale dei cambiamenti sulle relazioni di follow. */
    fun startRealtimeSync(currentUserEmail: String)

    /** Interrompe l'ascolto in tempo reale. */
    fun stopRealtimeSync()
}

/**
 * Implementazione concreta di [UserRepository] integrata con Room, Cloud Firestore e GeoFire.
 */
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val notificationRepository: NotificationRepository,
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var followsListener: ListenerRegistration? = null


    // ------------------------------------------------------------------------------------------------
    // Ricerca Utenti e Follow
    // ------------------------------------------------------------------------------------------------

    override fun searchUsersByUsername(query: String): Flow<List<UserEntity>> =
        userDao.searchUsersByUsername(query)

    override fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean> =
        userDao.isFollowing(followerEmail, followedEmail)

    override fun getFollowersCount(userEmail: String): Flow<Int> =
        userDao.getFollowersCount(userEmail)

    override fun getFollowingCount(userEmail: String): Flow<Int> =
        userDao.getFollowingCount(userEmail)

    override fun getFollowers(userEmail: String): Flow<List<UserEntity>> =
        userDao.getFollowers(userEmail)

    override fun getFollowing(userEmail: String): Flow<List<UserEntity>> =
        userDao.getFollowing(userEmail)

    override suspend fun toggleFollow(followerEmail: String, followedEmail: String) {
        try {
            val followId = "${followerEmail}_$followedEmail"
            val followRef = firestore.collection("follows").document(followId)
            if (followRef.get().await().exists()) {
                followRef.delete().await()
                userDao.deleteFollow(followerEmail, followedEmail)
            } else {
                followRef.set(mapOf("followerEmail" to followerEmail, "followedEmail" to followedEmail)).await()
                userDao.insertFollow(FollowEntity(followerEmail, followedEmail))

                notificationRepository.createNotification(
                    recipientEmail = followedEmail,
                    title = "Nuovo follow",
                    body = "$followerEmail ti segue",
                    route = "followers_list",
                    type = "follow",
                    senderEmail = followerEmail,
                    targetId = followerEmail,
                )
            }
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Errore toggle follow: ${e.message}")
        }
    }


    // ------------------------------------------------------------------------------------------------
    // Geolocalizzazione e Notifiche FCM
    // ------------------------------------------------------------------------------------------------

    override suspend fun saveFcmToken(userEmail: String, token: String) {
        if (userEmail.isBlank() || token.isBlank()) return
        firestore.collection("users").document(userEmail)
            .set(mapOf("fcmToken" to token), SetOptions.merge()).await()
    }

    override suspend fun updateUserLocation(userEmail: String, latitude: Double, longitude: Double) {
        val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude))
        firestore.collection("user_locations").document(userEmail)
            .set(mapOf("userEmail" to userEmail, "lat" to latitude, "lng" to longitude, "geohash" to hash)).await()
    }

    override suspend fun deleteUserLocation(userEmail: String) {
        try {
            firestore.collection("user_locations").document(userEmail).delete().await()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error deleting location: ${e.message}")
        }
    }

    override suspend fun findNearbyUsers(currentUserEmail: String, latitude: Double, longitude: Double, radiusKm: Int): List<GeoUser> {
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
                if (distanceM <= radiusM) {
                    userDao.getUserByEmail(email)?.let { result[email] = GeoUser(it, distanceM / 1000.0) }
                }
            }
        }
        return result.values.sortedBy { it.distanceKm }
    }


    // ------------------------------------------------------------------------------------------------
    // Sincronizzazione Remota e Pulizia
    // ------------------------------------------------------------------------------------------------

    override suspend fun syncUsersAndFollows(currentUserEmail: String) {
        try {
            val currentUserDoc = firestore.collection("users").document(currentUserEmail).get().await()
            currentUserDoc.toObject(UserEntity::class.java)?.let { userDao.insertUser(it) }

            val followingSnapshot = firestore.collection("follows").whereEqualTo("followerEmail", currentUserEmail).get().await()
            val followersSnapshot = firestore.collection("follows").whereEqualTo("followedEmail", currentUserEmail).get().await()

            val allFollows = followingSnapshot.documents + followersSnapshot.documents
            val followEntities = allFollows.mapNotNull { doc ->
                val follower = doc.getString("followerEmail")
                val followed = doc.getString("followedEmail")
                if (follower != null && followed != null) FollowEntity(follower, followed) else null
            }

            followEntities.forEach { userDao.insertFollow(it) }

            val relatedEmails = followEntities.flatMap { listOf(it.followerEmail, it.followedEmail) }
                .distinct()
                .filter { it != currentUserEmail }

            relatedEmails.chunked(10).forEach { chunk ->
                if (chunk.isNotEmpty()) {
                    val usersSnap = firestore.collection("users").whereIn("email", chunk).get().await()
                    userDao.insertUsers(usersSnap.toObjects(UserEntity::class.java))
                }
            }
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Sync Users fail: ${e.message}")
        }
    }

    override suspend fun clearLocalData() {
        try {
            database.clearAllTables()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error clearing local DB: ${e.message}")
        }
    }

    override fun startRealtimeSync(currentUserEmail: String) {
        stopRealtimeSync()
        followsListener = firestore.collection("follows").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            snapshot?.documentChanges?.forEach { change ->
                val follower = change.document.getString("followerEmail")
                val followed = change.document.getString("followedEmail")
                if (follower != null && followed != null && (follower == currentUserEmail || followed == currentUserEmail)) {
                    repositoryScope.launch {
                        if (change.type == DocumentChange.Type.ADDED) {
                            userDao.insertFollow(FollowEntity(follower, followed))
                        } else if (change.type == DocumentChange.Type.REMOVED) {
                            userDao.deleteFollow(follower, followed)
                        }
                    }
                }
            }
        }
    }

    override fun stopRealtimeSync() {
        followsListener?.remove()
    }
}
