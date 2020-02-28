package com.connectycube.messenger.data

import androidx.paging.DataSource
import androidx.room.*

/**
 * The Data Access Object for the Message class.
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(posts: List<Message>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(post: Message)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(item: Message): Int

    @Query("SELECT * FROM messages WHERE id = :id ")
    fun loadItem(id: String): Message

    @Query("UPDATE messages SET deliveredIds = :userId WHERE id = :id")
    fun updateDeliveredIds(id: String, userId: String): Int

    @Query("SELECT * FROM messages WHERE dialogId = :dialogId ORDER BY dateSent DESC")
    fun postsByDialogId(dialogId: String): DataSource.Factory<Int, Message>

    @Query("DELETE FROM messages WHERE dialogId = :dialogId")
    fun deleteByDialogId(dialogId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM messages")
    fun nukeTable()
}