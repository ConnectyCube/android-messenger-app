package com.connectycube.messenger.data

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeDialogType

@Entity(
    tableName = "chats",
    ignoredColumns = ["id", "customData", "type"]
)
data class Chat(
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "chat_id") val chatId: String,
    val dialogType: Int
) : ConnectycubeChatDialog() {
    init {
        type = ConnectycubeDialogType.parseByCode(dialogType)
    }

    fun getOccupantsIds(): List<Int>? {
        return occupants
    }

    override fun toString() =
        "chatId $chatId, lastMessageDateSent= $lastMessageDateSent, createdAt= $createdAt, lastMessage= $lastMessage, " +
                "updatedAt= $updatedAt, unreadMessageCount= $unreadMessageCount, name= $name, dialogType= $dialogType"
}