package com.connectycube.messenger

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.connectycube.messenger.helpers.AppNotificationManager
import com.connectycube.messenger.helpers.EXTRA_REPLY_TEXT
import com.connectycube.ConnectyCube
import com.connectycube.chat.models.ConnectycubeMessage
import java.util.concurrent.Executors


class SendFastReplyMessageService: Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        if (remoteInput != null) {
            val chatId = intent.getStringExtra(EXTRA_CHAT_ID)
            val replyText = remoteInput.getCharSequence(EXTRA_REPLY_TEXT)?.toString()

            val chatMessage = ConnectycubeMessage().apply {
                body = replyText
                dialogId = chatId
            }

            ConnectyCube.createMessage(chatMessage, true, {
                finishReplyProcess(applicationContext, chatId!!, replyText.toString(), true)
            }, {
                Toast.makeText(this@SendFastReplyMessageService, R.string.error_sending_message, Toast.LENGTH_SHORT).show()
                finishReplyProcess(applicationContext, chatId!!, replyText.toString(), false)
            })
        }

        return START_NOT_STICKY
    }

    private fun finishReplyProcess(context: Context, dialogId: String, text: String, isSuccess: Boolean){
        Executors.newSingleThreadExecutor().submit {
            AppNotificationManager.getInstance().notifyReplyResult(context, dialogId, text, isSuccess)
        }

        stopSelf()
    }
}