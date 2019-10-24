package com.connectycube.messenger.utilities

import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.data.Attachment
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
    return Chat(dialog.dialogId, dialog.type.code).apply {
        dialogId = dialog.dialogId
        lastMessage = dialog.lastMessage
        lastMessageDateSent = dialog.lastMessageDateSent
        lastMessageUserId = dialog.lastMessageUserId
        photo = dialog.photo
        userId = dialog.userId
        unreadMessageCount = dialog.unreadMessageCount ?: 0
        name = dialog.name
        setOccupantsIds(dialog.occupants)
        pinnedMessagesIds = dialog.pinnedMessagesIds
        type = dialog.type
        adminsIds = dialog.adminsIds
        customData = dialog.customData
        description = dialog.description
        occupantsCount = dialog.occupantsCount
        createdAt = dialog.createdAt
        updatedAt = dialog.updatedAt
    }
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

fun convertToAttachment(item: ConnectycubeAttachment, messageId: String): Attachment {
    return Attachment(item.id, messageId, item.type).apply {
        id = item.id
        contentType = item.contentType
        data = item.data
        duration = item.duration
        height = item.height
        width = item.width
        name = item.name
        size = item.size
        type = item.type
        url = item.url
    }
}

fun convertToListAttachment(item: ConnectycubeChatMessage):
        List<Attachment>? {
    item.attachments?.let {
        val result = mutableListOf<Attachment>()
        item.attachments.forEach {
            result.add(convertToAttachment(it, item.id))
        }
        return result
    }
    return null
}

fun convertToListOfListMessages(items: List<ConnectycubeChatMessage>): List<Attachment> {
    val attachments = mutableListOf<Attachment>()
    items.forEach { message ->
        val result = convertToListAttachment(message)
        result?.let { attachments.addAll(it) }
    }
    return attachments
}