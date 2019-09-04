package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.room.*

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
    fun getChat(chatId: String?): LiveData<Chat>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatValue(chatId: String?): Chat

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(chat: Chat)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(chat: Chat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(chats: List<Chat>)

    @Query("SELECT * FROM chats LIMIT :limit OFFSET :offset")
    fun getChatsByPage(limit: Int, offset: Int): LiveData<List<Chat>>

    @Query("DELETE FROM chats")
    fun nukeTable()

    @Query("DELETE FROM chats WHERE id in (:dialogsIds)")
    fun deleteChatsByIds(vararg dialogsIds: String?)

    @Delete
    fun deleteChat(vararg chats: Chat)
}