package com.example.checkpoint.data.repository

import com.example.checkpoint.data.local.dao.ChatDao
import com.example.checkpoint.data.local.dao.CommentDao
import com.example.checkpoint.data.local.dao.PostDao
import com.example.checkpoint.data.local.dao.UserDao
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.FollowEntity
import com.example.checkpoint.data.local.entity.MessageEntity
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
    
    // operazioni per follow
    fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean>
    suspend fun toggleFollow(followerEmail: String, followedEmail: String)
    fun getFollowersCount(userEmail: String): Flow<Int>
    fun getFollowingCount(userEmail: String): Flow<Int>
    fun getFollowers(userEmail: String): Flow<List<UserEntity>>
    fun getFollowing(userEmail: String): Flow<List<UserEntity>>

    // Operazioni per chat
    fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>>
    fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>>
    suspend fun sendMessage(message: MessageEntity)
    suspend fun markAsRead(currentUserEmail: String, otherUserEmail: String)
    fun hasUnreadMessages(currentUserEmail: String, otherUserEmail: String): Flow<Boolean>

    // Operazioni per commenti
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>
    suspend fun addComment(comment: CommentEntity)
    fun getCommentCount(postId: String): Flow<Int>
    suspend fun markCommentsAsRead(postId: String, currentUserEmail: String)
}

class MainRepositoryImpl(
    private val postDao: PostDao, 
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val commentDao: CommentDao
) : MainRepository {
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

    override fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>> {
        return chatDao.getChatPartners(currentUserEmail)
    }

    override fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>> {
        return chatDao.getChatMessages(user1, user2)
    }

    override suspend fun sendMessage(message: MessageEntity) {
        chatDao.insertMessage(message)
    }

    override suspend fun markAsRead(currentUserEmail: String, otherUserEmail: String) {
        chatDao.markAsRead(currentUserEmail, otherUserEmail)
    }

    override fun hasUnreadMessages(currentUserEmail: String, otherUserEmail: String): Flow<Boolean> {
        return chatDao.hasUnreadMessages(currentUserEmail, otherUserEmail)
    }

    override fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> {
        return commentDao.getCommentsForPost(postId)
    }

    override suspend fun addComment(comment: CommentEntity) {
        commentDao.insertComment(comment)
    }

    override fun getCommentCount(postId: String): Flow<Int> {
        return commentDao.getCommentCount(postId)
    }

    override suspend fun markCommentsAsRead(postId: String, currentUserEmail: String) {
        commentDao.markCommentsAsRead(postId, currentUserEmail)
    }
}
