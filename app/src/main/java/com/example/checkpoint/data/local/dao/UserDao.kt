package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.checkpoint.data.local.entity.FollowEntity
import com.example.checkpoint.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object per operazioni su utenti.
 */
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%'")
    fun searchUsersByUsername(query: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    // Operazioni di Follow
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE followerEmail = :followerEmail AND followedEmail = :followedEmail")
    suspend fun deleteFollow(followerEmail: String, followedEmail: String)

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerEmail = :followerEmail AND followedEmail = :followedEmail)")
    fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM follows WHERE followedEmail = :userEmail")
    fun getFollowersCount(userEmail: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM follows WHERE followerEmail = :userEmail")
    fun getFollowingCount(userEmail: String): Flow<Int>

    @Query("""
        SELECT users.* FROM users 
        INNER JOIN follows ON users.email = follows.followerEmail 
        WHERE follows.followedEmail = :userEmail
    """)
    fun getFollowers(userEmail: String): Flow<List<UserEntity>>

    @Query("""
        SELECT users.* FROM users 
        INNER JOIN follows ON users.email = follows.followedEmail 
        WHERE follows.followerEmail = :userEmail
    """)
    fun getFollowing(userEmail: String): Flow<List<UserEntity>>
}
