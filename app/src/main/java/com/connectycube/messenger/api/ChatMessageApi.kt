package com.connectycube.messenger.api

import com.connectycube.ConnectyCube
import com.connectycube.chat.models.ConnectycubeMessage
import com.connectycube.chat.queries.GetMessagesParameters
import com.connectycube.core.rest.request.RequestFilter
import com.connectycube.core.rest.request.RequestSorter

class ChatMessageApi {

    suspend fun getTop(dialogId: String, limit: Int): List<ConnectycubeMessage> {
        val messageGetBuilder: GetMessagesParameters = GetMessagesParameters().also { it.limit = limit; it.markAsRead = false; it.sorter = RequestSorter("", "date_sent", "desc")}

        return ConnectyCube.getMessages(dialogId, messageGetBuilder.getRequestParameters()).items
    }

    suspend fun getTopBefore(dialogId: String, limit: Int, before: Long): List<ConnectycubeMessage> {
        val messageGetBuilder: GetMessagesParameters = GetMessagesParameters().also { it.sorter = RequestSorter("", "date_sent", "desc")}

        messageGetBuilder.limit = limit
        messageGetBuilder.markAsRead = false
        messageGetBuilder.sorter = RequestSorter("", "date_sent", "desc")
        messageGetBuilder.filters = listOf(RequestFilter("", fieldName = "date_sent", fieldValue = before, "lt"))

        return ConnectyCube.getMessages(dialogId, messageGetBuilder.getRequestParameters()).items
    }

    suspend fun getTopAfter(dialogId: String, limit: Int, after: Long): List<ConnectycubeMessage> {
        val messageGetBuilder: GetMessagesParameters = GetMessagesParameters().also { it.sorter = RequestSorter("", "date_sent", "desc")}

        messageGetBuilder.limit = limit
        messageGetBuilder.markAsRead = false
        messageGetBuilder.sorter = RequestSorter("", "date_sent", "desc")
        messageGetBuilder.filters = listOf(RequestFilter("", fieldName = "date_sent", fieldValue = after, rule = "gt"))

        return ConnectyCube.getMessages(dialogId, messageGetBuilder.getRequestParameters()).items
    }
}