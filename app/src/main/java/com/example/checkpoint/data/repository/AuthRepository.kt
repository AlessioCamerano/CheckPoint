package com.example.checkpoint.data.repository

import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Repository dedicato all'autenticazione tramite Database Room.
 */
open class AuthRepository(private val userDao: UserDao?) {
    
    open suspend fun registerUser(user: UserEntity) {
        userDao?.insertUser(user)
    }

    open suspend fun updateUser(user: UserEntity) {
        userDao?.updateUser(user)
    }

    open suspend fun getUser(email: String): UserEntity? {
        return userDao?.getUserByEmail(email)
    }

    open suspend fun isEmailTaken(email: String): Boolean {
        return userDao?.getUserByEmail(email) != null
    }

    open suspend fun isUsernameTaken(username: String): Boolean {
        return userDao?.getUserByUsername(username) != null
    }

    open suspend fun loginUser(identifier: String): UserEntity? {
        return userDao?.getUserByIdentifier(identifier)
    }
}
