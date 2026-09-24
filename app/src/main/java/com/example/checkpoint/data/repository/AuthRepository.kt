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
 * Interfaccia che definisce le operazioni di autenticazione.
 */
interface AuthRepository {
    suspend fun registerUser(user: UserEntity)
    suspend fun updateUser(user: UserEntity)
    suspend fun getUser(email: String): UserEntity?
    suspend fun getUserByUsername(username: String): UserEntity?
    suspend fun isEmailTaken(email: String): Boolean
    suspend fun isUsernameTaken(username: String): Boolean

    // metodi autenticazione Firebase
    suspend fun signUpWithEmail(email: String, username: String, password: String): Result<UserEntity>
    suspend fun signInWithEmail(email: String, password: String): Result<UserEntity>
    suspend fun signInWithGoogle(idToken: String): Result<UserEntity>
    suspend fun logout()
    suspend fun deleteAccount(): Result<Unit>
    fun getCurrentUserEmail(): String?
}

/**
 * Autenticazione con Firebase e Cloud Firestore.
 */
class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {
    
    override suspend fun registerUser(user: UserEntity) {
        Log.d("CheckPointDebug", "Auth: Registrazione utente locale: ${user.email}")
        // Salva in locale
        userDao.insertUser(user)
        // Salva/Sincronizza su Firestore
        syncUserToFirestore(user)
    }

    override suspend fun updateUser(user: UserEntity) {
        Log.d("CheckPointDebug", "Auth: Aggiornamento utente locale: ${user.email}")
        userDao.updateUser(user)
        syncUserToFirestore(user)
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

    override suspend fun getUser(email: String): UserEntity? {
        // Prova prima in locale
        val localUser = userDao.getUserByEmail(email)
        
        // Anche se lo trova in locale, prova a sincronizzarlo su Firestore
        // per essere sicuro che il cloud sia aggiornato
        if (localUser != null) {
            try {
                syncUserToFirestore(localUser)
            } catch (e: Exception) {
                Log.e("CheckPointDebug", "Auth: Errore sync background utente su Firestore: ${e.message}")
            }
            return localUser
        }

        Log.d("CheckPointDebug", "Auth: Utente non trovato in Room, cerco su FIRESTORE: $email")
        // Se non c'è, prova su Firestore
        return try {
            val doc = firestore.collection("users").document(email).get().await()
            val remoteUser = doc.toObject(UserEntity::class.java)
            if (remoteUser != null) {
                Log.d("CheckPointDebug", "Auth: Utente trovato su FIRESTORE, salvo in cache locale")
                userDao.insertUser(remoteUser) // Salva in cache locale
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

    override suspend fun signUpWithEmail(email: String, username: String, password: String): Result<UserEntity> {
        Log.d("CheckPointDebug", "Auth: Tentativo Signup Firebase Email: $email")
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Log.d("CheckPointDebug", "Auth: Signup Firebase SUCCESS")
            val userEntity = UserEntity(email = email, username = username)
            registerUser(userEntity) // Usa registerUser per salvare sia in Room che su Firestore
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
            
            // Carica profilo da Firestore o lo crea
            val user = getUser(email) ?: UserEntity(
                email = email,
                username = firebaseUser.displayName ?: email.split("@")[0]
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
            
            val email = firebaseUser.email ?: throw Exception("Email not provided by Google")
            
            // Carica profilo da Firestore o lo crea
            val user = getUser(email) ?: UserEntity(
                email = email,
                username = firebaseUser.displayName ?: email.split("@")[0]
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
            try { firestore.collection("user_locations").document(email).delete().await() } catch (e: Exception) { Log.e("CheckPointDebug", "Error deleting user_location: ${e.message}") }
            
            // 2. Cancella documento utente da Firestore
            try { firestore.collection("users").document(email).delete().await() } catch (e: Exception) { Log.e("CheckPointDebug", "Error deleting user doc: ${e.message}") }

            // 3. Cancella post creati dall'utente
            try {
                val userPosts = firestore.collection("posts").whereEqualTo("authorEmail", email).get().await()
                for (doc in userPosts.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) { Log.e("CheckPointDebug", "Error deleting user posts: ${e.message}") }

            // 4. Cancella notifiche dell'utente
            try {
                val notifs = firestore.collection("notifications").whereEqualTo("recipientEmail", email).get().await()
                for (doc in notifs.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) { Log.e("CheckPointDebug", "Error deleting user notifications: ${e.message}") }

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
}
