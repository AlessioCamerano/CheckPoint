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
 * Accesso ai dati per gli utenti e le relazioni di seguito (follow).
 */
@Dao
interface UserDao {

    // ------------------------------------------------------------------------------------------------
    // Gestione Profili Utente
    // ------------------------------------------------------------------------------------------------

    /** Salva o aggiorna un utente. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /** Salva o aggiorna una lista di utenti. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    /** Aggiorna i dati di un utente esistente. */
    @Update
    suspend fun updateUser(user: UserEntity)

    /** Cerca un utente per email. */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    /** Cerca un utente per username. */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    /** Cerca gli utenti il cui username contiene la stringa fornita. */
    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%'")
    fun searchUsersByUsername(query: String): Flow<List<UserEntity>>


    // ------------------------------------------------------------------------------------------------
    // Gestione Follower / Following
    // ------------------------------------------------------------------------------------------------

    /** Aggiunge una relazione di follow tra due utenti. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    /** Rimuove una relazione di follow tra due utenti. */
    @Query("DELETE FROM follows WHERE followerEmail = :followerEmail AND followedEmail = :followedEmail")
    suspend fun deleteFollow(followerEmail: String, followedEmail: String)

    /** Verifica se l'utente follower segue l'utente seguito. */
    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerEmail = :followerEmail AND followedEmail = :followedEmail)")
    fun isFollowing(followerEmail: String, followedEmail: String): Flow<Boolean>

    /** Numero di follower di un utente. */
    @Query("SELECT COUNT(*) FROM follows WHERE followedEmail = :userEmail")
    fun getFollowersCount(userEmail: String): Flow<Int>

    /** Numero di utenti seguiti da un utente. */
    @Query("SELECT COUNT(*) FROM follows WHERE followerEmail = :userEmail")
    fun getFollowingCount(userEmail: String): Flow<Int>

    /** Lista dei follower di un utente. */
    @Query("""
        SELECT users.* FROM users 
        INNER JOIN follows ON users.email = follows.followerEmail 
        WHERE follows.followedEmail = :userEmail
    """)
    fun getFollowers(userEmail: String): Flow<List<UserEntity>>

    /** Lista degli utenti seguiti da un utente. */
    @Query("""
        SELECT users.* FROM users 
        INNER JOIN follows ON users.email = follows.followedEmail 
        WHERE follows.followerEmail = :userEmail
    """)
    fun getFollowing(userEmail: String): Flow<List<UserEntity>>
}
