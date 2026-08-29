package com.example.checkpoint.data.repository

import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.FollowEntity
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository per la gestione dei post e dei dati principali.
 */
interface MainRepository {
    fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun getAllUniqueTags(): Flow<List<String>>
    fun getTrendingTags(): Flow<List<String>>
    fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun searchUsersByUsername(query: String): Flow<List<UserEntity>>
    suspend fun createPost(post: Post, tags: List<String>)
    suspend fun toggleLike(postId: String, userEmail: String)
    
    // operazioni di follow
    fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean>
    suspend fun toggleFollow(followerEmail: String, followedEmail: String)
    fun getFollowersCount(userEmail: String): Flow<Int>
    fun getFollowingCount(userEmail: String): Flow<Int>
    fun getFollowers(userEmail: String): Flow<List<UserEntity>>
    fun getFollowing(userEmail: String): Flow<List<UserEntity>>
}

class MainRepositoryImpl(private val postDao: PostDao, private val userDao: UserDao) : MainRepository {
    override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> {
        return postDao.getAllPostsWithAuthor(currentUserEmail)
    }

    override fun getAllUniqueTags(): Flow<List<String>> {
        return postDao.getAllUniqueTags()
    }

    override fun getTrendingTags(): Flow<List<String>> {
        return postDao.getTrendingTags()
    }

    override fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> {
        return postDao.searchPostsByTitle(query, currentUserEmail)
    }

    override fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>> {
        return postDao.searchPostsByTag(query, currentUserEmail)
    }

    override fun searchUsersByUsername(query: String): Flow<List<UserEntity>> {
        return userDao.searchUsersByUsername(query)
    }

    override suspend fun createPost(post: Post, tags: List<String>) {
        val tagEntities = tags.map { PostTagEntity(post.id, it) }
        postDao.insertPostWithTags(post, tagEntities)
    }

    override suspend fun toggleLike(postId: String, userEmail: String) {
        postDao.toggleLike(postId, userEmail)
    }

    override fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean> {
        return userDao.isFollowing(followerEmail, followedEmail)
    }

    override suspend fun toggleFollow(followerEmail: String, followedEmail: String) {
        val currentlyFollowing = userDao.isFollowing(followerEmail, followedEmail).first()
        if (currentlyFollowing) {
            userDao.deleteFollow(followerEmail, followedEmail)
        } else {
            userDao.insertFollow(FollowEntity(followerEmail, followedEmail))
        }
    }

    override fun getFollowersCount(userEmail: String): Flow<Int> {
        return userDao.getFollowersCount(userEmail)
    }

    override fun getFollowingCount(userEmail: String): Flow<Int> {
        return userDao.getFollowingCount(userEmail)
    }

    override fun getFollowers(userEmail: String): Flow<List<UserEntity>> {
        return userDao.getFollowers(userEmail)
    }

    override fun getFollowing(userEmail: String): Flow<List<UserEntity>> {
        return userDao.getFollowing(userEmail)
    }
}
