package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.PostLikeEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import kotlinx.coroutines.flow.Flow

/**
 * Accesso ai dati per post, tag e preferiti (like).
 */
@Dao
interface PostDao {

    // ------------------------------------------------------------------------------------------------
    // Salvataggio Post e Tag
    // ------------------------------------------------------------------------------------------------

    /** Salva o aggiorna un post. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    /** Salva una lista di post. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    /** Inserisce una lista di tag ignorando i duplicati. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<PostTagEntity>)

    /** Salva in modo atomico un post insieme ai suoi tag. */
    @Transaction
    suspend fun insertPostWithTags(post: PostEntity, tags: List<PostTagEntity>) {
        insertPost(post)
        if (tags.isNotEmpty()) {
            insertTags(tags)
        }
    }


    // ------------------------------------------------------------------------------------------------
    // Lettura e Ricerca Post
    // ------------------------------------------------------------------------------------------------

    /** Recupera tutti i post con autore e stato "like" dell'utente corrente. */
    @Transaction
    @Query("""
        SELECT posts.*, 
        (SELECT COUNT(*) FROM post_likes WHERE postId = posts.id) AS favoriteCount,
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser
        FROM posts 
        ORDER BY timestamp DESC
    """)
    fun getAllPostsWithAuthor(currentUserEmail: String): Flow<List<PostWithAuthor>>

    /** Cerca un singolo post tramite il suo ID. */
    @Transaction
    @Query("""
        SELECT posts.*, 
        (SELECT COUNT(*) FROM post_likes WHERE postId = posts.id) AS favoriteCount,
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser
        FROM posts 
        WHERE posts.id = :postId
    """)
    suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor?

    /** Cerca i post per titolo, autore o messaggio iniziale. */
    @Transaction
    @Query("""
        SELECT posts.*, 
        (SELECT COUNT(*) FROM post_likes WHERE postId = posts.id) AS favoriteCount,
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser
        FROM posts 
        INNER JOIN users ON posts.authorEmail = users.email
        WHERE posts.title LIKE '%' || :query || '%' 
           OR users.username LIKE '%' || :query || '%' 
           OR posts.initialMessage LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>

    /** Cerca i post associati a uno specifico tag. */
    @Transaction
    @Query("""
        SELECT DISTINCT posts.*, 
        (SELECT COUNT(*) FROM post_likes WHERE postId = posts.id) AS favoriteCount,
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser
        FROM posts 
        INNER JOIN post_tags ON posts.id = post_tags.postId 
        WHERE post_tags.tag LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>


    // ------------------------------------------------------------------------------------------------
    // Gestione Tag
    // ------------------------------------------------------------------------------------------------

    /** Cerca i tag unici che corrispondono al testo fornito. */
    @Query("SELECT DISTINCT tag FROM post_tags WHERE tag LIKE '%' || :query || '%'")
    fun searchUniqueTags(query: String): Flow<List<String>>

    /** I 5 tag più popolari in base al numero di utilizzi. */
    @Query("""
        SELECT tag
        FROM post_tags 
        GROUP BY tag 
        ORDER BY COUNT(*) DESC 
        LIMIT 5
    """)
    fun getTrendingTags(): Flow<List<String>>


    // ------------------------------------------------------------------------------------------------
    // Gestione Like / Preferiti
    // ------------------------------------------------------------------------------------------------

    /** Aggiunge un like a un post. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: PostLikeEntity)

    /** Rimuove un like da un post. */
    @Query("DELETE FROM post_likes WHERE postId = :postId AND userEmail = :userEmail")
    suspend fun deleteLike(postId: String, userEmail: String)

    /** Verifica se l'utente ha messo like al post. */
    @Query("SELECT EXISTS(SELECT 1 FROM post_likes WHERE postId = :postId AND userEmail = :userEmail)")
    suspend fun isPostLikedByUser(postId: String, userEmail: String): Boolean

    /** Alterna lo stato del like (aggiunge se assente, rimuove se presente). */
    @Transaction
    suspend fun toggleLike(postId: String, userEmail: String) {
        if (isPostLikedByUser(postId, userEmail)) {
            deleteLike(postId, userEmail)
        } else {
            insertLike(PostLikeEntity(postId, userEmail))
        }
    }


    // ------------------------------------------------------------------------------------------------
    // Eliminazione Post
    // ------------------------------------------------------------------------------------------------

    /** Elimina un post dato il suo ID. */
    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    /** Elimina tutti i post locali non presenti nella lista degli ID forniti. */
    @Query("DELETE FROM posts WHERE id NOT IN (:validPostIds)")
    suspend fun deletePostsNotIn(validPostIds: List<String>)
}
