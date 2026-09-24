package com.example.checkpoint.data.repository

import android.util.Log
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Gestione delle operazioni di autenticazione, registrazione e sincronizzazione del profilo utente.
 */
interface AuthRepository {

    // ------------------------------------------------------------------------------------------------
    // Autenticazione Firebase
    // ------------------------------------------------------------------------------------------------

    /** Registrazione tramite email, username e password. */
    suspend fun signUpWithEmail(email: String, username: String, password: String): Result<UserEntity>

    /** Accesso tramite email e password. */
    suspend fun signInWithEmail(email: String, password: String): Result<UserEntity>

    /** Accesso o registrazione tramite account Google. */
    suspend fun signInWithGoogle(idToken: String): Result<UserEntity>

    /** Disconnette l'utente corrente da Firebase. */
    suspend fun logout()

    /** Elimina l'account utente e tutti i suoi dati associati. */
    suspend fun deleteAccount(): Result<Unit>

    /** Restituisce l'email dell'utente attualmente autenticato. */
    fun getCurrentUserEmail(): String?


    // ------------------------------------------------------------------------------------------------
    // Gestione e Verifica Profilo Utente
    // ------------------------------------------------------------------------------------------------

    /** Registra un nuovo utente sia in locale (Room) sia su Cloud Firestore. */
    suspend fun registerUser(user: UserEntity)

    /** Aggiorna i dati del profilo sia in locale (Room) sia su Cloud Firestore. */
    suspend fun updateUser(user: UserEntity)

    /** Cerca un utente per email, controllando prima la cache locale e poi Firestore. */
    suspend fun getUser(email: String): UserEntity?

    /** Cerca un utente per username. */
    suspend fun getUserByUsername(username: String): UserEntity?

    /** Verifica se un'email è già registrata nel DB locale. */
    suspend fun isEmailTaken(email: String): Boolean

    /** Verifica se uno username è già in uso nel DB locale. */
    suspend fun isUsernameTaken(username: String): Boolean
}

/**
 * Implementazione concreta di [AuthRepository] integrata con Room, Firebase Auth e Firestore.
 */
class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    // ------------------------------------------------------------------------------------------------
    // Autenticazione Firebase
    // ------------------------------------------------------------------------------------------------

    override suspend fun signUpWithEmail(email: String, username: String, password: String): Result<UserEntity> {
        Log.d("CheckPointDebug", "Auth: Tentativo Signup Firebase Email: $email")
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Log.d("CheckPointDebug", "Auth: Signup Firebase SUCCESS")
            val userEntity = UserEntity(email = email, username = username)
            registerUser(userEntity)
            Result.success(userEntity)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Auth: ERRORE Signup Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserEntity> {
        Log.d("CheckPointDebug", "Auth: Tentativo Login Firebase Email: $email")
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign-in failed")
            Log.d("CheckPointDebug", "Auth: Login Firebase SUCCESS")

            val user = getUser(email) ?: UserEntity(
                email = email,
                username = firebaseUser.displayName ?: email.split("@")[0],
            ).also { registerUser(it) }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Auth: ERRORE Login Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserEntity> {
        Log.d("CheckPointDebug", "Auth: Tentativo Login Firebase GOOGLE")
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google Sign-in failed")
            Log.d("CheckPointDebug", "Auth: Login GOOGLE SUCCESS")

            val email = firebaseUser.email ?: throw Exception("Email non fornita da Google")

            val user = getUser(email) ?: UserEntity(
                email = email,
                username = firebaseUser.displayName ?: email.split("@")[0],
            ).also { registerUser(it) }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Auth: ERRORE Login GOOGLE: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        Log.d("CheckPointDebug", "Auth: Esecuzione LOGOUT")
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val email = getCurrentUserEmail()
        val user = firebaseAuth.currentUser
        Log.d("CheckPointDebug", "Auth: Esecuzione ELIMINAZIONE ACCOUNT per $email")
        if (email.isNullOrBlank() || user == null) {
            return Result.failure(Exception("Utente non autenticato"))
        }

        return try {
            // 1. Cancella posizione utente da Firestore
            try {
                firestore.collection("user_locations").document(email).delete().await()
            } catch (e: Exception) {
                Log.e("CheckPointDebug", "Error deleting user_location: ${e.message}")
            }

            // 2. Cancella documento utente da Firestore
            try {
                firestore.collection("users").document(email).delete().await()
            } catch (e: Exception) {
                Log.e("CheckPointDebug", "Error deleting user doc: ${e.message}")
            }

            // 3. Cancella post creati dall'utente
            try {
                val userPosts = firestore.collection("posts").whereEqualTo("authorEmail", email).get().await()
                for (doc in userPosts.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.e("CheckPointDebug", "Error deleting user posts: ${e.message}")
            }

            // 4. Cancella notifiche dell'utente
            try {
                val notifs = firestore.collection("notifications").whereEqualTo("recipientEmail", email).get().await()
                for (doc in notifs.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.e("CheckPointDebug", "Error deleting user notifications: ${e.message}")
            }

            // 5. Cancella utente da Firebase Auth
            user.delete().await()
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Log.e("CheckPointDebug", "Auth: Re-authentication required to delete account", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Auth: ERRORE eliminazione account: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }


    // ------------------------------------------------------------------------------------------------
    // Gestione e Verifica Profilo Utente
    // ------------------------------------------------------------------------------------------------

    override suspend fun registerUser(user: UserEntity) {
        Log.d("CheckPointDebug", "Auth: Registrazione utente locale: ${user.email}")
        userDao.insertUser(user)
        syncUserToFirestore(user)
    }

    override suspend fun updateUser(user: UserEntity) {
        Log.d("CheckPointDebug", "Auth: Aggiornamento utente locale: ${user.email}")
        userDao.updateUser(user)
        syncUserToFirestore(user)
    }

    override suspend fun getUser(email: String): UserEntity? {
        val localUser = userDao.getUserByEmail(email)

        if (localUser != null) {
            try {
                syncUserToFirestore(localUser)
            } catch (e: Exception) {
                Log.e("CheckPointDebug", "Auth: Errore sync background utente su Firestore: ${e.message}")
            }
            return localUser
        }

        Log.d("CheckPointDebug", "Auth: Utente non trovato in Room, cerco su FIRESTORE: $email")
        return try {
            val doc = firestore.collection("users").document(email).get().await()
            val remoteUser = doc.toObject(UserEntity::class.java)
            if (remoteUser != null) {
                Log.d("CheckPointDebug", "Auth: Utente trovato su FIRESTORE, salvo in cache locale")
                userDao.insertUser(remoteUser)
            } else {
                Log.d("CheckPointDebug", "Auth: Utente NON trovato neanche su FIRESTORE")
            }
            remoteUser
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Auth: ERRORE recupero utente FIRESTORE: ${e.message}", e)
            null
        }
    }

    override suspend fun getUserByUsername(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    override suspend fun isEmailTaken(email: String): Boolean {
        return userDao.getUserByEmail(email) != null
    }

    override suspend fun isUsernameTaken(username: String): Boolean {
        return userDao.getUserByUsername(username) != null
    }

    private suspend fun syncUserToFirestore(user: UserEntity) {
        Log.d("CheckPointDebug", "Auth: Sincronizzazione utente su FIRESTORE: ${user.email}")
        try {
            firestore.collection("users").document(user.email).set(user).await()
            Log.d("CheckPointDebug", "Auth: Utente sincronizzato con successo su FIRESTORE")
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Auth: ERRORE sincronizzazione FIRESTORE: ${e.message}", e)
        }
    }
}
