package com.example.checkpoint.data.repository

import android.util.Log
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
    fun getCurrentUserEmail(): String?
}

/**
 * Autenticazione con Firebase.
 */
class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {
    
    override suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    override suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    override suspend fun getUser(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
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
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val userEntity = UserEntity(email = email, username = username)
            userDao.insertUser(userEntity)
            Result.success(userEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserEntity> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign-in failed")
            
            // controllo che l'utente esista in Room
            var user = userDao.getUserByEmail(email)
            if (user == null) {
                user = UserEntity(
                    email = email,
                    username = firebaseUser.displayName ?: email.split("@")[0]
                )
                userDao.insertUser(user)
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserEntity> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google Sign-in failed")
            
            val email = firebaseUser.email ?: throw Exception("Email not provided by Google")
            var user = userDao.getUserByEmail(email)
            if (user == null) {
                user = UserEntity(
                    email = email,
                    username = firebaseUser.displayName ?: email.split("@")[0]
                )
                userDao.insertUser(user)
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firebase Google sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }
}
