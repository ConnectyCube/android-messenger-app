package com.connectycube.messenger.data

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.connectycube.chat.model.ConnectycubeChatDialog

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id") val chatId: String,
    val lastMessageDateSent: Long,
    val createdAt: Long,
    val unreadMessageCount: Int,
    val name: String,
    val conChat: ConnectycubeChatDialog
) {
    override fun toString() = "chatId $chatId, lastMessageDateSent= $lastMessageDateSent, createdAt= $createdAt, " +
            "unreadMessageCount= $unreadMessageCount, name= $name"
}