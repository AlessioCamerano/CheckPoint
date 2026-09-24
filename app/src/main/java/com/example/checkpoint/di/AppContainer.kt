package com.example.checkpoint.di

import android.content.Context
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.AuthRepositoryImpl
import com.example.checkpoint.data.repository.ChatRepository
import com.example.checkpoint.data.repository.ChatRepositoryImpl
import com.example.checkpoint.data.repository.NotificationRepository
import com.example.checkpoint.data.repository.NotificationRepositoryImpl
import com.example.checkpoint.data.repository.PostRepository
import com.example.checkpoint.data.repository.PostRepositoryImpl
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.repository.UserPreferencesRepositoryImpl
import com.example.checkpoint.data.repository.UserRepository
import com.example.checkpoint.data.repository.UserRepositoryImpl

/**
 * Contenitore delle dipendenze dell'applicazione per la Dependency Injection manuale.
 */
interface AppContainer {
    val authRepository: AuthRepository
    val preferencesRepository: UserPreferencesRepository
    val notificationRepository: NotificationRepository
    val userRepository: UserRepository
    val postRepository: PostRepository
    val chatRepository: ChatRepository
}

/**
 * Implementazione predefinita di [AppContainer] che crea e gestisce le istanze Lazy dei Repository e del Database.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    // ------------------------------------------------------------------------------------------------
    // Database Locale
    // ------------------------------------------------------------------------------------------------

    private val database by lazy { AppDatabase.getDatabase(context) }


    // ------------------------------------------------------------------------------------------------
    // Istanze dei Repository (Iniezione Lazy)
    // ------------------------------------------------------------------------------------------------

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(database.userDao())
    }

    override val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(context)
    }

    override val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl()
    }

    override val userRepository: UserRepository by lazy {
        UserRepositoryImpl(database.userDao(), notificationRepository, database)
    }

    override val postRepository: PostRepository by lazy {
        PostRepositoryImpl(database.postDao(), database.commentDao(), database.userDao(), notificationRepository)
    }

    override val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(database.chatDao(), notificationRepository)
    }
}
