package com.connectycube.messenger.data

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.connectycube.chat.model.ConnectycubeChatMessage

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id") val messageId: String,
    val dialogId: String,
    val dateSent: Long,
    val cubeMessage: ConnectycubeChatMessage
) {
    override fun toString() = "messageId= $messageId, body= ${cubeMessage.body}, dateSent= $dateSent"
}