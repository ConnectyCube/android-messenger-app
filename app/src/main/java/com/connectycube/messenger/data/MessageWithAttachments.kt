package com.connectycube.messenger.data

import androidx.room.Embedded
import androidx.room.Relation

data class MessageWithAttachments(
    @Embedded
    val message: Message,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val attachments: List<Attachment>?
) {
    init {
        message.attachments = attachments
    }

    override fun toString() =
        "messageId= ${message.messageId}, body= ${message.body}, attachments= $attachments"
}