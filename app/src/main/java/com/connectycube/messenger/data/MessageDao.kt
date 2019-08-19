package com.connectycube.messenger.data

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The Data Access Object for the Message class.
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(posts: List<Message>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(post: Message)

    @Query("SELECT * FROM messages WHERE dialogId = :dialogId ORDER BY dateSent DESC")
    fun postsByDialogId(dialogId: String): DataSource.Factory<Int, Message>

    @Query("DELETE FROM messages WHERE id = :dialogId")
    fun deleteByDialogId(dialogId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM messages")
    fun nukeTable()
}