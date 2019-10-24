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

    override fun equals(other: Any?): Boolean {
        return this === other || other is ConnectycubeChatDialog && other.dialogId == this.dialogId
                && other.lastMessageDateSent == this.lastMessageDateSent
                && other.lastMessage == this.lastMessage
                && other.unreadMessageCount == this.unreadMessageCount
                && other.name == this.name
                && other.createdAt == this.createdAt
                && other.updatedAt == this.updatedAt
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString() =
        "chatId $chatId, lastMessageDateSent= $lastMessageDateSent, createdAt= $createdAt, lastMessage= $lastMessage, " +
                "updatedAt= $updatedAt, unreadMessageCount= $unreadMessageCount, name= $name, dialogType= $dialogType"
}