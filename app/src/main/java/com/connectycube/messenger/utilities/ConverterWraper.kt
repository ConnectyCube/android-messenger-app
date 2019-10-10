package com.connectycube.messenger.utilities

import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.Message
import com.connectycube.messenger.data.User
import com.connectycube.users.model.ConnectycubeUser
import java.util.*


fun convertToUsers(list: ArrayList<ConnectycubeUser>): List<User> {
    val users = ArrayList<User>()
    list.forEach { users.add(convertToUser(it)) }
    return users
}

fun convertToUser(user: ConnectycubeUser): User {
    return User(user.id, user.login, user.fullName, user)
}

fun convertToChats(list: ArrayList<ConnectycubeChatDialog>): List<Chat> {
    val chats = ArrayList<Chat>()
    list.forEach {
        chats.add(convertToChat(it))
    }
    return chats
}

fun convertToChat(dialog: ConnectycubeChatDialog): Chat {
    return Chat(
        dialog.dialogId,
        dialog.lastMessageDateSent,
        dialog.createdAt.time,
        dialog.updatedAt.time,
        dialog.unreadMessageCount ?: 0,
        dialog.name,
        dialog
    )
}

fun convertToMessages(items: ArrayList<ConnectycubeChatMessage>): List<Message> {
    val result: ArrayList<Message> = ArrayList()
    items.forEach {
        result.add(convertToMessage(it))
    }
    return result
}

fun convertToMessage(item: ConnectycubeChatMessage): Message {
    return Message(item.id).apply {
        id = item.id
        body = item.body
        dialogId = item.dialogId
        dateSent = item.dateSent
        senderId = item.senderId
        recipientId = item.recipientId
        readIds = item.readIds
        deliveredIds = item.deliveredIds
        attachments = item.attachments
    }
}