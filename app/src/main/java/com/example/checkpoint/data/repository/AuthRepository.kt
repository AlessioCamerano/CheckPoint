package com.example.checkpoint.data.repository

import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Repository dedicato all'autenticazione tramite Database Room.
 */
class AuthRepository(private val userDao: UserDao) {
    
    suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun getUser(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
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
