package com.connectycube.messenger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.connectycube.chat.models.ConnectycubeAttachment

@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey
    @ColumnInfo(name = "attachment_id") val attachmentId: String,
    val messageId: String,
    val attachmentType: String
) : ConnectycubeAttachment(attachmentType) {
    override fun toString() = "attachmentId= $attachmentId, url= $url"
}