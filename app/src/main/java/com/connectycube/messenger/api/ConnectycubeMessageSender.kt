package com.connectycube.messenger.api

import android.content.Context
import android.widget.Toast
import com.connectycube.messenger.R
import com.connectycube.ConnectyCube
import com.connectycube.chat.models.ConnectycubeAttachment
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeDialogType
import com.connectycube.chat.models.ConnectycubeMessage
import timber.log.Timber

class ConnectycubeMessageSender(private val context: Context, private val dialog: ConnectycubeDialog) {

    fun sendChatMessage(
        text: String? = "",
        attachment: ConnectycubeAttachment? = null
    ): Pair<Boolean, ConnectycubeMessage> {
        val chatMessage = ConnectycubeMessage()
        var result: Boolean = false
        if (attachment != null) {
            chatMessage.attachments!!.add(attachment)
            chatMessage.body = context.getString(R.string.message_attachment)
        } else {
            chatMessage.body = text
        }
        chatMessage.saveToHistory = true
        chatMessage.dateSent = System.currentTimeMillis() / 1000
        chatMessage.markable = true

        if(dialog.type == ConnectycubeDialogType.PRIVATE) chatMessage.recipientId = dialog.getRecipientId()

        try {
            ConnectyCube.chat.sendMessage(chatMessage)
            result = true
        } catch (e: Exception) {
            Timber.d(e)
            Toast.makeText(context, "Can't send a message, You are not connected to chat", Toast.LENGTH_SHORT).show()
        } finally {
            return Pair(result, chatMessage)
        }
    }

}