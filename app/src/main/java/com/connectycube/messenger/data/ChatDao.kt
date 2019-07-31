package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The Data Access Object for the Chat class.
 */
@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageDateSent DESC")
    fun getChatsSync(): List<Chat>

    @Query("SELECT * FROM chats ORDER BY lastMessageDateSent DESC")
    fun getChats(): LiveData<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChat(chatId: Int): LiveData<Chat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(chat: Chat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(chats: List<Chat>)
}