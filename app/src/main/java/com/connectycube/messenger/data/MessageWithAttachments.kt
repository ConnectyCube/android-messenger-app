package com.connectycube.messenger.data

import androidx.room.Embedded
import androidx.room.Relation

data class MessageWithAttachments(
    @Embedded
    val message: Message,
    @Relation(parentColumn = "message_id", entityColumn = "messageId", entity = Attachment::class)
    val attachments: List<Attachment>?
) {
    init {
        if (attachments != null) {
            message.attachments = attachments.toCollection(ArrayList())
        }
    }

    override fun toString() =
        "messageId= ${message.messageID}, body= ${message.body}, attachments= $attachments"
}