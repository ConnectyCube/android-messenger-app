package com.connectycube.messenger.api

import android.content.Context
import android.widget.Toast
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.messenger.R
import org.jivesoftware.smack.SmackException
import timber.log.Timber

class ConnectycubeMessageSender(private val context: Context, private val dialog: ConnectycubeChatDialog) {


    fun sendChatMessage(
        text: String? = "",
        attachment: ConnectycubeAttachment? = null
    ): Pair<Boolean, ConnectycubeChatMessage> {
        val chatMessage = ConnectycubeChatMessage()
        var result: Boolean = false
        if (attachment != null) {
            chatMessage.addAttachment(attachment)
            chatMessage.body = context.getString(R.string.message_attachment)
        } else {
            chatMessage.body = text
        }
        chatMessage.setSaveToHistory(true)
        chatMessage.dateSent = System.currentTimeMillis() / 1000
        chatMessage.isMarkable = true

        if (ConnectycubeDialogType.PRIVATE != dialog.getType() && !dialog.isJoined()) {
            Toast.makeText(context, "You're not joined a group chat yet, try again later", Toast.LENGTH_SHORT).show()
            return Pair(result, chatMessage)
        }

        try {
            dialog.sendMessage(chatMessage)
            result = true
        } catch (e: SmackException.NotConnectedException) {
            Timber.d(e)
            Toast.makeText(context, "Can't send a message, You are not connected to chat", Toast.LENGTH_SHORT).show()
        } catch (e: InterruptedException) {
            Timber.d(e)
            Toast.makeText(context, "Can't send a message, You are not connected to chat", Toast.LENGTH_SHORT).show()
        } finally {
            return Pair(result, chatMessage)
        }
    }

}