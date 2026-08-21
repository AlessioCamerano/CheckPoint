package com.example.checkpoint.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetUser() = runBlocking {
        val user = UserEntity("test@gmail.com", "testuser", "password123")
        userDao.insertUser(user)
        
        val retrievedUser = userDao.getUserByEmail("test@gmail.com")
        assertNotNull(retrievedUser)
        assertEquals(user.username, retrievedUser?.username)
    }

    @Test
    fun updateAndGetUser() = runBlocking {
        val user = UserEntity("test@gmail.com", "testuser", "password123")
        userDao.insertUser(user)
        
        val updatedUser = user.copy(username = "newname", bio = "I am a dev")
        userDao.updateUser(updatedUser)
        
        val retrievedUser = userDao.getUserByEmail("test@gmail.com")
        assertEquals("newname", retrievedUser?.username)
        assertEquals("I am a dev", retrievedUser?.bio)
    }
}
