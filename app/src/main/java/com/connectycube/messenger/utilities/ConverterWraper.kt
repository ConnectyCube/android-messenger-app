package com.connectycube.messenger.utilities

import com.connectycube.messenger.data.Attachment
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.Message
import com.connectycube.messenger.data.User
import com.connectycube.chat.models.ConnectycubeAttachment
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeMessage
import com.connectycube.users.models.ConnectycubeUser
import java.util.*


fun convertToUsers(list: List<ConnectycubeUser>): List<User> {
    val users = ArrayList<User>()
    list.forEach { users.add(convertToUser(it)) }
    return users
}

fun convertToUser(user: ConnectycubeUser): User {
    return User(user.id, user.login!!, user.fullName ?: "fullName", user)
}

fun convertToChats(list: List<ConnectycubeDialog>): List<Chat> {
    val chats = ArrayList<Chat>()
    list.forEach {
        chats.add(convertToChat(it))
    }
    return chats
}

fun convertToChat(dialog: ConnectycubeDialog): Chat {
    return Chat(dialog.dialogId!!, dialog.type).apply {
        dialogId = dialog.dialogId
        lastMessage = dialog.lastMessage
        lastMessageDateSent = dialog.lastMessageDateSent
        lastMessageUserId = dialog.lastMessageUserId
        photo = dialog.photo
        userId = dialog.userId
        unreadMessageCount = dialog.unreadMessageCount ?: 0
        name = dialog.name
        occupantsIds = dialog.occupantsIds
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

fun convertToMessages(items: List<ConnectycubeMessage>): List<Message> {
    val result: ArrayList<Message> = ArrayList()
    items.forEach {
        result.add(convertToMessage(it))
    }
    return result
}

fun convertToMessage(item: ConnectycubeMessage): Message {
    return Message(item.messageId!!).apply {
        messageId = item.messageId
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
    return Attachment(item.id!!, messageId, item.type!!).apply {
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

fun convertToListAttachment(item: ConnectycubeMessage):
        List<Attachment>? {
    item.attachments?.let {
        val result = mutableListOf<Attachment>()
        item.attachments!!.forEach {
            result.add(convertToAttachment(it, item.messageId!!))
        }
        return result
    }
    return null
}

fun convertToListOfListMessages(items: List<ConnectycubeMessage>): List<Attachment> {
    val attachments = mutableListOf<Attachment>()
    items.forEach { message ->
        val result = convertToListAttachment(message)
        result?.let { attachments.addAll(it) }
    }
    return attachments
}