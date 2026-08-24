package com.example.checkpoint.data.repository

import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.entity.Post
import kotlinx.coroutines.flow.Flow

/**
 * Repository per la gestione dei post e dei dati principali.
 */
interface MainRepository {
    fun getAllPosts(): Flow<List<Post>>
    suspend fun createPost(post: Post)
}

class MainRepositoryImpl(private val postDao: PostDao) : MainRepository {
    override fun getAllPosts(): Flow<List<Post>> {
        return postDao.getAllPosts()
    }

    override suspend fun createPost(post: Post) {
        postDao.insertPost(post)
    }
}
