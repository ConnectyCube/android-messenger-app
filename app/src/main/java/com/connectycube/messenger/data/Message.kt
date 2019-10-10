package com.connectycube.messenger.data

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.connectycube.chat.model.ConnectycubeChatMessage

@Entity(
    tableName = "messages",
    ignoredColumns = ["properties", "packetExtension", "complexProperties", "saveToHistory", "delayed"]
)
data class Message(
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id") val messageId: String
) :
    ConnectycubeChatMessage() {

    override fun toString() = "messageId= $messageId, body= $body, dateSent= $dateSent"
}