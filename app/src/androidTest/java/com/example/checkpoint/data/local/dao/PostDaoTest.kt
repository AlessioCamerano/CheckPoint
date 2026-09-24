package com.example.checkpoint.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test strumentati per le operazioni di I/O del database Room in-memory per [PostDao].
 */
@RunWith(AndroidJUnit4::class)
class PostDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var postDao: PostDao
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        postDao = database.postDao()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }


    // ------------------------------------------------------------------------------------------------
    // Test Inserimento, Lettura ed Eliminazione Post
    // ------------------------------------------------------------------------------------------------

    @Test
    fun insertAndGetAllPosts() = runBlocking {
        val user1 = UserEntity("user1@gmail.com", "User1")
        val user2 = UserEntity("user2@gmail.com", "User2")
        userDao.insertUser(user1)
        userDao.insertUser(user2)

        val post1 = PostEntity(
            authorEmail = "user1@gmail.com",
            title = "Title 1",
            initialMessage = "Message 1",
            timestamp = 1000L,
            mediaUri = "content://img1",
            mediaType = MediaType.IMAGE,
        )
        val post2 = PostEntity(
            authorEmail = "user2@gmail.com",
            title = "Title 2",
            initialMessage = "Message 2",
            timestamp = 2000L,
            mediaUri = null,
            mediaType = MediaType.NONE,
        )
        postDao.insertPost(post1)
        postDao.insertPost(post2)

        val allPosts = postDao.getAllPostsWithAuthor("user1@gmail.com").first()
        assertEquals(2, allPosts.size)
        assertEquals("Title 2", allPosts[0].post.title)
        assertEquals("Title 1", allPosts[1].post.title)
    }

    @Test
    fun deletePost() = runBlocking {
        val user = UserEntity("user@gmail.com", "User")
        userDao.insertUser(user)
        val post = PostEntity(id = "test-id", authorEmail = "user@gmail.com", title = "Title", initialMessage = "Message")
        postDao.insertPost(post)

        postDao.deletePost("test-id")

        val allPosts = postDao.getAllPostsWithAuthor("user@gmail.com").first()
        assertTrue(allPosts.isEmpty())
    }
}
