package com.example.checkpoint.data

import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Gestisce le operazioni di autenticazione
 */
class AuthRepository(private val userDao: UserDao) {
    
    suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
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
