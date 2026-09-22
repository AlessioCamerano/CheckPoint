package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.model.CommentWithAuthor
import kotlinx.coroutines.flow.Flow

/**
 * DAO per i commenti.
 */
@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    // Recupera i commenti di un post in ordine cronologico con l'autore
    @Transaction
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>>

    // Conteggio totale commenti
    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    fun getCommentCount(postId: String): Flow<Int>
    @Transaction
    @Query("SELECT comments.* FROM comments ORDER BY timestamp DESC")
    fun getAllCommentsFlow(): Flow<List<CommentWithAuthor>>
}
