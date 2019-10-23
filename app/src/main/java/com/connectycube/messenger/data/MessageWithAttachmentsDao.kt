package com.connectycube.messenger.data

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query

@Dao
interface MessageWithAttachmentsDao {
    @Query("SELECT * from messages")
    fun getMessagesWithAttachments(): List<MessageWithAttachments>

    @Query("SELECT * FROM messages WHERE dialogId = :dialogId ORDER BY dateSent DESC")
    fun postsByDialogId(dialogId: String): DataSource.Factory<Int, MessageWithAttachments>
}