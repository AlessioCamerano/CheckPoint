package com.example.checkpoint.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.Post
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var postDao: PostDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        postDao = database.postDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetAllPosts() = runBlocking {
        val post1 = Post(
            authorName = "User1",
            authorEmail = "user1@gmail.com",
            title = "Title 1",
            initialMessage = "Message 1",
            timestamp = 1000L
        )
        val post2 = Post(
            authorName = "User2",
            authorEmail = "user2@gmail.com",
            title = "Title 2",
            initialMessage = "Message 2",
            timestamp = 2000L
        )

        postDao.insertPost(post1)
        postDao.insertPost(post2)

        val allPosts = postDao.getAllPosts().first()

        assertEquals(2, allPosts.size)
        // controlla l'ordine, in base al timestamp
        assertEquals("Title 2", allPosts[0].title)
        assertEquals("Title 1", allPosts[1].title)
    }

    @Test
    fun deletePost() = runBlocking {
        val post = Post(
            id = "test-id",
            authorName = "User",
            authorEmail = "user@gmail.com",
            title = "Title",
            initialMessage = "Message"
        )
        postDao.insertPost(post)
        
        postDao.deletePost("test-id")
        
        val allPosts = postDao.getAllPosts().first()
        assertTrue(allPosts.isEmpty())
    }
}
