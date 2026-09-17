package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.checkpoint.data.local.entity.CommentEntity
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

    // Recupera i commenti di un post in ordine cronologico
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    // Conteggio totale commenti
    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    fun getCommentCount(postId: String): Flow<Int>

    // Segna i commenti di un post come letti
    @Query("UPDATE comments SET isRead = 1 WHERE postId = :postId AND authorEmail != :currentUserEmail")
    suspend fun markCommentsAsRead(postId: String, currentUserEmail: String)

    // Verifica la presenza di nuovi commenti
    @Query("SELECT EXISTS(SELECT 1 FROM comments WHERE postId = :postId AND isRead = 0 AND authorEmail != :currentUserEmail)")
    fun hasUnreadComments(postId: String, currentUserEmail: String): Flow<Boolean>
}
