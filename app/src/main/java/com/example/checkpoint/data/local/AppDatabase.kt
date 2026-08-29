package com.example.checkpoint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.FollowEntity
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.PostLikeEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Punto cdi accesso a Room
 */
@Database(
    entities = [UserEntity::class, Post::class, PostLikeEntity::class, PostTagEntity::class, FollowEntity::class], 
    version = 8, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao

    companion object {
        //Logica Singleton
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "checkpoint_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true) // Cancella e ricrea se lo schema cambia
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
