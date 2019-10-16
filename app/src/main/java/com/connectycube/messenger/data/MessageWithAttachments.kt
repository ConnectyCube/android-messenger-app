package com.connectycube.messenger.data

import androidx.room.Embedded
import androidx.room.Relation


class MessageWithAttachments {
    @Embedded
    lateinit var message: Message

    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    var attachments: List<Attachment>? = null

    override fun toString() =
        "messageId= ${message.messageId}, body= ${message.body}, attachments= $attachments"
}