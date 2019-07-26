package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The Data Access Object for the User class.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name")
    fun getUsersSync(): List<User>

    @Query("SELECT * FROM users ORDER BY name")
    fun getUsers(): LiveData<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: Int): LiveData<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<User>)
}