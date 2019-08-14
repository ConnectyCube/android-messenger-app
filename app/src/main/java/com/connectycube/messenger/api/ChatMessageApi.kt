package com.connectycube.messenger.api

import com.connectycube.chat.ConnectycubeRestChatService
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.request.MessageGetBuilder
import com.connectycube.core.server.Performer
import java.util.ArrayList

class ChatMessageApi {

    fun getTop(dialogId: String, limit: Int): Performer<ArrayList<ConnectycubeChatMessage>> {
        val messageGetBuilder = MessageGetBuilder()

        messageGetBuilder.limit = limit
        messageGetBuilder.sortDesc("date_sent")
        messageGetBuilder.markAsRead(false)

        return ConnectycubeRestChatService.getDialogMessages(ConnectycubeChatDialog(dialogId), messageGetBuilder)
    }

    fun getTopBefore(dialogId: String, limit: Int, before: Long): Performer<ArrayList<ConnectycubeChatMessage>> {
        val messageGetBuilder = MessageGetBuilder()

        messageGetBuilder.limit = limit
        messageGetBuilder.sortDesc("date_sent")
        messageGetBuilder.lt("date_sent", before)
        messageGetBuilder.markAsRead(false)

        return ConnectycubeRestChatService.getDialogMessages(ConnectycubeChatDialog(dialogId), messageGetBuilder)
    }

    fun getTopAfter(dialogId: String, limit: Int, after: Long): Performer<ArrayList<ConnectycubeChatMessage>> {
        val messageGetBuilder = MessageGetBuilder()

        messageGetBuilder.limit = limit
        messageGetBuilder.sortDesc("date_sent")
        messageGetBuilder.gt("date_sent", after)
        messageGetBuilder.markAsRead(false)

        return ConnectycubeRestChatService.getDialogMessages(ConnectycubeChatDialog(dialogId), messageGetBuilder)
    }
}