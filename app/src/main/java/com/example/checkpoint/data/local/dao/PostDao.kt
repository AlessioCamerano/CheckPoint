package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.checkpoint.data.local.entity.Post
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object per i Post.
 */
@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)
}
