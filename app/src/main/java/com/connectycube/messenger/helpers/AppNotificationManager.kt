package com.connectycube.messenger.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.messenger.*
import com.connectycube.messenger.data.AppDatabase
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.utilities.SharedPreferencesManager
import java.util.*


const val PARAM_NOTIFICATION_TYPE = "push_notification_type"
const val PARAM_MESSAGE = "message"
const val PARAM_USER_ID = "user_id"
const val PARAM_MESSAGE_ID = "message_id"
const val PARAM_DIALOG_ID = "dialog_id"
const val PARAM_CALL_ID = "notification_type"
const val PARAM_ANSWER_TIMEOUT = "answer_timeout"

const val CALLS_CHANNEL_ID = "notifications_channel_calls"
const val CALL_NOTIFICATION_ID = 222

const val CHATS_CHANNEL_ID = "notifications_channel_chats"
const val CHATS_NOTIFICATION_ID = 333

const val CHATS_NOTIFICATION_GROUP_ID = "chat_notifications_group_id"

const val EXTRA_REPLY_TEXT = "key_reply"
const val EXTRA_NOTIFICATION_ID = "notification_id"

const val NOTIFICATION_TYPE_CALL = 2

class AppNotificationManager {

    private val notificationsMessages = mutableMapOf<String, NotificationCompat.MessagingStyle>()

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AppNotificationManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: AppNotificationManager().also { instance = it }
            }
    }

    fun processPushNotification(context: Context, data: Map<String, String>) {
        if (isChatMessage(data)) {
            showChatNotification(context, data)
            return
        }

        if (data.containsKey(PARAM_NOTIFICATION_TYPE)) {
            when (data[PARAM_NOTIFICATION_TYPE]?.toInt()) {
                NOTIFICATION_TYPE_CALL -> {
                    showCallNotification(context, data)
                }
            }
        }
    }

    private fun showChatNotification(context: Context, data: Map<String, String>) {
        val chtRepository = ChatRepository.getInstance(AppDatabase.getInstance(context.applicationContext).chatDao())
        val dialogId = data[PARAM_DIALOG_ID]
        val dialog: Chat? = chtRepository.getChatSync(dialogId!!)
        val notificationId = dialogId.hashCode()
        val dialogName = dialog?.name ?: dialogId
        val startBundle = Bundle().apply {
            putString(EXTRA_CHAT_ID, dialogId)
            putInt(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val intent =
            prepareStartIntent(context, ChatMessageActivity::class.java, startBundle).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK /*or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY */
            }

        val pendingIntent: PendingIntent =
            PendingIntent.getActivities(context, Random().nextInt(), arrayOf(getDefaultActivityIntent(context), intent), FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChatNotificationsChannel(context)
        }

        val remoteInput = RemoteInput.Builder(EXTRA_REPLY_TEXT)
            .setLabel(context.getString(R.string.enter_your_reply_here))
            .build()

        val replyPendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PendingIntent.getService(
                    context,
                    Random().nextInt(),
                    Intent(context, SendFastReplyMessageService::class.java).apply {
                        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                        putExtra(EXTRA_CHAT_ID, dialogId)
                    },
                    FLAG_ONE_SHOT
                )
            } else {
                pendingIntent
            }

        val replyAction =
            NotificationCompat.Action.Builder(
                0,
                context.getString(R.string.reply),
                replyPendingIntent
            )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()

        val builder = prepareSimpleNotificationBuilder(context, CHATS_CHANNEL_ID)
            .setContentTitle(dialogName)
            .setContentText(data[PARAM_MESSAGE])
            .setContentIntent(pendingIntent)
            .setStyle(getMessageStyleForDialog(dialogId, dialogName, data.getValue(PARAM_MESSAGE)))
            .addAction(replyAction)

        displayNotification(context, notificationId, builder.build())
    }

    private fun getMessageStyleForDialog(dialogId: String, dialogName: String, newMessage: CharSequence): NotificationCompat.MessagingStyle {
        var messagingStyle = notificationsMessages[dialogId]
        if (messagingStyle == null) {
            messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName(dialogName).build())
            notificationsMessages[dialogId] = messagingStyle
        }

        messagingStyle.addMessage(newMessage, Date().time, Person.Builder().setName(dialogName).build())

        return messagingStyle
    }

    private fun isChatMessage(data: Map<String, String>): Boolean {
        return data.containsKey(PARAM_MESSAGE_ID) && data.containsKey(PARAM_DIALOG_ID)
    }

    private fun showCallNotification(context: Context, data: Map<String, String>) {
        if (ConnectycubeChatService.getInstance().isLoggedIn) return

        val intent = prepareStartIntent(context, LoginActivity::class.java, Bundle())

        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createCallsNotificationsChannel(context)
        }

        val builder = prepareSimpleNotificationBuilder(context, CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_incoming_call)
            .setContentTitle(context.getString(R.string.incoming_call))
            .setContentText(data[PARAM_MESSAGE])
            .setContentIntent(pendingIntent)

        val answerTimeout = data[PARAM_ANSWER_TIMEOUT]?.toLong()
        if (answerTimeout != null && answerTimeout > 0) {
            builder.setTimeoutAfter(answerTimeout)
        }

        displayNotification(context, CALL_NOTIFICATION_ID, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCallsNotificationsChannel(context: Context) {
        val name = context.getString(R.string.calls_channel_name)
        val descriptionText = context.getString(R.string.calls_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CALLS_CHANNEL_ID, name, importance)
        channel.description = descriptionText
        configureAndFireNotificationChannel(context, channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChatNotificationsChannel(context: Context) {
        val name = context.getString(R.string.chats_channel_name)
        val descriptionText = context.getString(R.string.chats_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHATS_CHANNEL_ID, name, importance)
        channel.description = descriptionText
        configureAndFireNotificationChannel(context, channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun configureAndFireNotificationChannel(context: Context,
                                                    channel: NotificationChannel
    ) {
        channel.vibrationPattern = longArrayOf(500)
        channel.enableVibration(true)
        channel.lightColor = context.resources.getColor(R.color.colorPrimary)
        channel.enableLights(true)
        getNotificationManager(context).createNotificationChannel(channel)
    }

    private fun getNotificationManager(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }

    private fun prepareSimpleNotificationBuilder(context: Context,
                                                 channelId: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(500))
            .setLights(context.resources.getColor(R.color.colorPrimary), 2000, 2000)
            .setColor(context.resources.getColor(R.color.colorPrimary))
            .setAutoCancel(true)
    }

    private fun displayNotification(context: Context,
                                    notificationId: Int,
                                    notification: Notification
    ) {
        getNotificationManager(context).notify(notificationId, notification)
    }

    private fun prepareStartIntent(context: Context,
                                   clazz: Class<out AppCompatActivity>,
                                   extras: Bundle
    ): Intent {
        return if (userAlreadyRegistered(context)) {
            Intent(context, clazz).apply { putExtras(extras) }
        } else {
            getDefaultActivityIntent(context)
        }
    }

    private fun getDefaultActivityIntent(context: Context): Intent {
        return  Intent(context, LoginActivity::class.java)
    }

    private fun userAlreadyRegistered(context: Context): Boolean {
        return SharedPreferencesManager.getInstance(context).currentUserExists()
    }

    fun notifyReplyResult(context: Context, notificationId: Int, dialogId: String, text: String, success: Boolean) {
        if (success){
            val params = mutableMapOf<String, String>()
            params[PARAM_DIALOG_ID] = dialogId
            params[PARAM_MESSAGE] = context.getString(R.string.you_format, text)
            showChatNotification(context, params)
        } else {
            //TODO VT back there
            Toast.makeText(context, R.string.error_sending_message, Toast.LENGTH_SHORT).show()
            getNotificationManager(context).cancel(notificationId)
        }
    }

    fun clearNotificationData(context: Context, dialogId: String){
        getNotificationManager(context).cancel(dialogId.hashCode())

        notificationsMessages.remove(dialogId)
    }

}