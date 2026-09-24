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
 * Accesso ai dati per i commenti dei post.
 */
@Dao
interface CommentDao {

    // ------------------------------------------------------------------------------------------------
    // Salvataggio Commenti
    // ------------------------------------------------------------------------------------------------

    /** Salva un singolo commento. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    /** Salva una lista di commenti. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)


    // ------------------------------------------------------------------------------------------------
    // Lettura Commenti
    // ------------------------------------------------------------------------------------------------

    /** Commenti di un post in ordine cronologico con i dati dell'autore. */
    @Transaction
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>>

    /** Numero totale di commenti per un post. */
    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    fun getCommentCount(postId: String): Flow<Int>
}
