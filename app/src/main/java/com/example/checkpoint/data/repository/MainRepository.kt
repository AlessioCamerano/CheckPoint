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
    suspend fun createPost(post: Post)
    suspend fun toggleLike(postId: String, userEmail: String)
}

class MainRepositoryImpl(private val postDao: PostDao) : MainRepository {
    override fun getAllPosts(currentUserEmail: String): Flow<List<PostWithAuthor>> {
        return postDao.getAllPostsWithAuthor(currentUserEmail)
    }

    override suspend fun createPost(post: Post) {
        postDao.insertPost(post)
    }

    override suspend fun toggleLike(postId: String, userEmail: String) {
        postDao.toggleLike(postId, userEmail)
    }
}
