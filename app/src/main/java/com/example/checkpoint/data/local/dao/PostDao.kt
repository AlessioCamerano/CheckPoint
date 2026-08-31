package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.PostLikeEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object per i Post.
 */
@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<PostTagEntity>)

    @Transaction
    suspend fun insertPostWithTags(post: Post, tags: List<PostTagEntity>) {
        insertPost(post)
        if (tags.isNotEmpty()) {
            insertTags(tags)
        }
    }

    @Transaction
    @Query("""
        SELECT posts.*, 
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser,
        EXISTS(SELECT 1 FROM comments WHERE postId = posts.id AND isRead = 0 AND authorEmail != :currentUserEmail) AS hasUnreadComments
        FROM posts 
        ORDER BY timestamp DESC
    """)
    fun getAllPostsWithAuthor(currentUserEmail: String): Flow<List<PostWithAuthor>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: PostLikeEntity)

    @Query("DELETE FROM post_likes WHERE postId = :postId AND userEmail = :userEmail")
    suspend fun deleteLike(postId: String, userEmail: String)

    @Query("UPDATE posts SET favoriteCount = favoriteCount + 1 WHERE id = :postId")
    suspend fun incrementFavoriteCount(postId: String)

    @Query("UPDATE posts SET favoriteCount = favoriteCount - 1 WHERE id = :postId")
    suspend fun decrementFavoriteCount(postId: String)

    @Transaction
    suspend fun toggleLike(postId: String, userEmail: String) {
        val isLiked = isPostLikedByUser(postId, userEmail)
        if (isLiked) {
            deleteLike(postId, userEmail)
            decrementFavoriteCount(postId)
        } else {
            insertLike(PostLikeEntity(postId, userEmail))
            incrementFavoriteCount(postId)
        }
    }

    @Query("SELECT DISTINCT tag FROM post_tags")
    fun getAllUniqueTags(): Flow<List<String>>

    @Query("""
        SELECT tag
        FROM post_tags 
        GROUP BY tag 
        ORDER BY COUNT(*) DESC 
        LIMIT 5
    """)
    fun getTrendingTags(): Flow<List<String>>

    @Transaction
    @Query("""
        SELECT posts.*, 
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser,
        EXISTS(SELECT 1 FROM comments WHERE postId = posts.id AND isRead = 0 AND authorEmail != :currentUserEmail) AS hasUnreadComments
        FROM posts 
        WHERE title LIKE '%' || :query || '%' OR authorName LIKE '%' || :query || '%' OR initialMessage LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>

    @Transaction
    @Query("""
        SELECT DISTINCT posts.*, 
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser,
        EXISTS(SELECT 1 FROM comments WHERE postId = posts.id AND isRead = 0 AND authorEmail != :currentUserEmail) AS hasUnreadComments
        FROM posts 
        INNER JOIN post_tags ON posts.id = post_tags.postId 
        WHERE post_tags.tag LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>

    @Query("SELECT EXISTS(SELECT 1 FROM post_likes WHERE postId = :postId AND userEmail = :userEmail)")
    suspend fun isPostLikedByUser(postId: String, userEmail: String): Boolean

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)
}
