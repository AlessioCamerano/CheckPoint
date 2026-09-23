package com.example.checkpoint.di

import android.content.Context
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.repository.*

/**
 * Contenitore delle dipendenze per l'iniezione manuale.
 * Garantisce che esista una singola istanza globale dei Repository e del Database.
 */
interface AppContainer {
    val authRepository: AuthRepository
    val preferencesRepository: UserPreferencesRepository
    val notificationRepository: NotificationRepository
    val userRepository: UserRepository
    val postRepository: PostRepository
    val chatRepository: ChatRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    // L'istanza viene creata solo al primo effettivo utilizzo.
    private val database by lazy { AppDatabase.getDatabase(context) }

    override val authRepository: AuthRepository by lazy { AuthRepositoryImpl(database.userDao()) }
    override val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepositoryImpl(context) }
    override val notificationRepository: NotificationRepository by lazy { NotificationRepositoryImpl() }

    // Dipendenze incrociate direttamente nel costruttore
    override val userRepository: UserRepository by lazy { UserRepositoryImpl(database.userDao(), notificationRepository, database) }
    override val postRepository: PostRepository by lazy { PostRepositoryImpl(database.postDao(), database.commentDao(), notificationRepository) }
    override val chatRepository: ChatRepository by lazy { ChatRepositoryImpl(database.chatDao(), notificationRepository) }
}