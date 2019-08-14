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
    list.forEach { users.add(User(it.id, it.login, it.fullName, it)) }
    return users
}

fun convertToChats(list: ArrayList<ConnectycubeChatDialog>): List<Chat> {
    val chats = ArrayList<Chat>()
    list.forEach {
        chats.add(
            Chat(
                it.dialogId,
                it.lastMessageDateSent,
                it.createdAt.time,
                it.unreadMessageCount,
                it.name,
                it
            )
        )
    }
    return chats
}

fun convertToMessages(items: ArrayList<ConnectycubeChatMessage>): List<Message> {
    val result: ArrayList<Message> = ArrayList()
    items.forEach {
        result.add(convertToMessage(it))
    }
    return result
}

fun convertToMessage(item: ConnectycubeChatMessage): Message {
    return Message(item.id, item.dialogId, item.dateSent, item)
}