package com.example.checkpoint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.checkpoint.data.local.dao.ChatDao
import com.example.checkpoint.data.local.dao.CommentDao
import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.FollowEntity
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.PostLikeEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Database principale Room dell'applicazione CheckPoint per il salvataggio e la cache locale dei dati.
 */
@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        PostLikeEntity::class,
        PostTagEntity::class,
        FollowEntity::class,
        MessageEntity::class,
        CommentEntity::class,
    ],
    version = 15,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    // ------------------------------------------------------------------------------------------------
    // Data Access Objects (DAO)
    // ------------------------------------------------------------------------------------------------

    /** Restituisce l'istanza per le operazioni sugli utenti. */
    abstract fun userDao(): UserDao

    /** Restituisce l'istanza per le operazioni sui post, tag e like. */
    abstract fun postDao(): PostDao

    /** Restituisce l'istanza per le operazioni sulla chat e messaggi. */
    abstract fun chatDao(): ChatDao

    /** Restituisce l'istanza per le operazioni sui commenti. */
    abstract fun commentDao(): CommentDao


    // ------------------------------------------------------------------------------------------------
    // Singleton Instance
    // ------------------------------------------------------------------------------------------------

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Restituisce l'istanza Singleton thread-safe del database Room.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "checkpoint_database",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
