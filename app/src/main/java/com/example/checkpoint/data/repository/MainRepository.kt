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
    suspend fun createPost(post: Post, tags: List<String>)
    suspend fun toggleLike(postId: String, userEmail: String)
}

class MainRepositoryImpl(private val postDao: PostDao) : MainRepository {
    override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> {
        return postDao.getAllPostsWithAuthor(currentUserEmail)
    }

    override fun getAllUniqueTags(): Flow<List<String>> {
        return postDao.getAllUniqueTags()
    }

    override suspend fun createPost(post: Post, tags: List<String>) {
        val tagEntities = tags.map { com.example.checkpoint.data.local.entity.PostTagEntity(post.id, it) }
        postDao.insertPostWithTags(post, tagEntities)
    }

    override suspend fun toggleLike(postId: String, userEmail: String) {
        postDao.toggleLike(postId, userEmail)
    }
}
