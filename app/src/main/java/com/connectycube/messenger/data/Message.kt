package com.connectycube.messenger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.connectycube.chat.models.ConnectycubeMessage

@Entity(
    tableName = "messages",
    ignoredColumns = ["properties", "isRead", "saveToHistory", "delayed", "attachments", "reactions"]
)
data class Message(
    @PrimaryKey
    @ColumnInfo(name = "message_id") val messageID: String
) :
    ConnectycubeMessage() {

    override fun toString() = "messageId= $messageID, body= $body, dateSent= $dateSent " + super.toString()
}