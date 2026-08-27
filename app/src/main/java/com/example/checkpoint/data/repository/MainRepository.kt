package com.example.checkpoint.data.repository

import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.model.PostWithAuthor
import kotlinx.coroutines.flow.Flow

/**
 * Repository per la gestione dei post e dei dati principali.
 */
interface MainRepository {
    fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun getAllUniqueTags(): Flow<List<String>>
    fun getTrendingTags(): Flow<List<String>>
    fun searchPostsByTitle(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun searchPostsByTag(query: String, currentUserEmail: String): Flow<List<PostWithAuthor>>
    fun searchUsersByUsername(query: String): Flow<List<com.example.checkpoint.data.local.entity.UserEntity>>
    suspend fun createPost(post: Post, tags: List<String>)
    suspend fun toggleLike(postId: String, userEmail: String)
}

class MainRepositoryImpl(private val postDao: PostDao, private val userDao: com.example.checkpoint.data.local.dao.UserDao) : MainRepository {
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

    override fun searchUsersByUsername(query: String): Flow<List<com.example.checkpoint.data.local.entity.UserEntity>> {
        return userDao.searchUsersByUsername(query)
    }

    override suspend fun createPost(post: Post, tags: List<String>) {
        val tagEntities = tags.map { com.example.checkpoint.data.local.entity.PostTagEntity(post.id, it) }
        postDao.insertPostWithTags(post, tagEntities)
    }

    override suspend fun toggleLike(postId: String, userEmail: String) {
        postDao.toggleLike(postId, userEmail)
    }
}
