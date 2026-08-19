package com.example.checkpoint.data

import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.preferences.SessionManager
import kotlinx.coroutines.flow.Flow

/**
 * Gestisce le operazioni di autenticazione
 */
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    
    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn
    val userEmail: Flow<String?> = sessionManager.userEmail

    suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun getUser(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    suspend fun saveSession(email: String) {
        sessionManager.saveSession(email)
    }

    suspend fun clearSession() {
        sessionManager.clearSession()
    }

    suspend fun isEmailTaken(email: String): Boolean {
        return userDao.getUserByEmail(email) != null
    }

    suspend fun isUsernameTaken(username: String): Boolean {
        return userDao.getUserByUsername(username) != null
    }

    suspend fun loginUser(identifier: String): UserEntity? {
        return userDao.getUserByIdentifier(identifier)
    }
}
