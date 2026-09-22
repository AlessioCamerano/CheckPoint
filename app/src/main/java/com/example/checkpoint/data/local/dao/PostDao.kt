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
 * Data Access Object per i PostEntity.
 */
@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<PostTagEntity>)

    @Transaction
    suspend fun insertPostWithTags(post: PostEntity, tags: List<PostTagEntity>) {
        insertPost(post)
        if (tags.isNotEmpty()) {
            insertTags(tags)
        }
    }

    @Transaction
    @Query("""
        SELECT posts.*, 
        (SELECT COUNT(*) FROM post_likes WHERE postId = posts.id) AS favoriteCount,
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser
        FROM posts 
        ORDER BY timestamp DESC
    """)
    fun getAllPostsWithAuthor(currentUserEmail: String): Flow<List<PostWithAuthor>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: PostLikeEntity)

    @Query("DELETE FROM post_likes WHERE postId = :postId AND userEmail = :userEmail")
    suspend fun deleteLike(postId: String, userEmail: String)

    @Transaction
    suspend fun toggleLike(postId: String, userEmail: String) {
        val isLiked = isPostLikedByUser(postId, userEmail)
        if (isLiked) {
            deleteLike(postId, userEmail)
        } else {
            insertLike(PostLikeEntity(postId, userEmail))
        }
    }

    @Query("SELECT DISTINCT tag FROM post_tags WHERE tag LIKE '%' || :query || '%'")
    fun searchUniqueTags(query: String): Flow<List<String>>

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
        (SELECT COUNT(*) FROM post_likes WHERE postId = posts.id) AS favoriteCount,
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser
        FROM posts 
        INNER JOIN users ON posts.authorEmail = users.email
        WHERE posts.title LIKE '%' || :query || '%' OR users.username LIKE '%' || :query || '%' OR posts.initialMessage LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>

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

    @Query("SELECT EXISTS(SELECT 1 FROM post_likes WHERE postId = :postId AND userEmail = :userEmail)")
    suspend fun isPostLikedByUser(postId: String, userEmail: String): Boolean

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Transaction
    @Query("""
        SELECT posts.*, 
        (SELECT COUNT(*) FROM post_likes WHERE postId = posts.id) AS favoriteCount,
        EXISTS(SELECT 1 FROM post_likes WHERE postId = posts.id AND userEmail = :currentUserEmail) AS isLikedByCurrentUser
        FROM posts 
        WHERE posts.id = :postId
    """)
    suspend fun getPostById(postId: String, currentUserEmail: String): PostWithAuthor?
}
