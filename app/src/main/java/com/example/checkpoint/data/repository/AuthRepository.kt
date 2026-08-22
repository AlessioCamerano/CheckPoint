package com.example.checkpoint.data.repository

import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Interfaccia che definisce le operazioni di autenticazione.
 */
interface AuthRepository {
    suspend fun registerUser(user: UserEntity)
    suspend fun updateUser(user: UserEntity)
    suspend fun getUser(email: String): UserEntity?
    suspend fun isEmailTaken(email: String): Boolean
    suspend fun isUsernameTaken(username: String): Boolean
    suspend fun loginUser(identifier: String): UserEntity?
}

/**
 * Implementazione concreta dell'autenticazione con Database Room.
 */
class AuthRepositoryImpl(private val userDao: UserDao) : AuthRepository {
    
    override suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    override suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    override suspend fun getUser(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    override suspend fun isEmailTaken(email: String): Boolean {
        return userDao.getUserByEmail(email) != null
    }

    override suspend fun isUsernameTaken(username: String): Boolean {
        return userDao.getUserByUsername(username) != null
    }

    override suspend fun loginUser(identifier: String): UserEntity? {
        return userDao.getUserByIdentifier(identifier)
    }
}
