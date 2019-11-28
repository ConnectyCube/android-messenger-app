package com.connectycube.messenger.data

import androidx.room.*


@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(posts: List<Attachment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(post: Attachment)

    @Query("SELECT * FROM attachments WHERE messageId = :messageId ")
    fun loadItemsByMessageId(messageId: String): List<Attachment>

    @Query("SELECT * FROM attachments WHERE attachment_id = :attachId ")
    fun loadItemsById(attachId: String): List<Attachment>

    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM attachments WHERE attachment_id = :attachmentId")
    fun deleteById(attachmentId: String)

    @Delete
    fun delete(posts: List<Attachment>)

    @Query("UPDATE attachments SET name=:newName WHERE attachment_id = :attachmentId")
    fun updateId(attachmentId: String?, newName: String)

    @Transaction
    fun insertAndDeleteInTransaction(messageId: String, newAttachments: List<Attachment>) {
        // Anything inside this method runs in a single transaction.
        deleteByMessageId(messageId)
        insert(newAttachments)
    }

    @Query("DELETE FROM attachments")
    fun nukeTable()
}