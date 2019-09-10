package com.connectycube.messenger.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.messenger.LoginActivity
import com.connectycube.messenger.R

const val PARAM_NOTIFICATION_TYPE = "push_notification_type"
const val PARAM_MESSAGE = "message"
const val PARAM_CALL_ID = "notification_type"

const val CALLS_CHANNEL_ID = "notifications_channel_calls"
const val CALL_NOTIFICATION_ID = 222

const val NOTIFICATION_TYPE_CALL = 2

class AppNotificationManager {

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AppNotificationManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: AppNotificationManager().also { instance = it }
            }
    }

    fun processPushNotification(context: Context, data: Map<String, String>){
        if (data.containsKey(PARAM_NOTIFICATION_TYPE)){
            when(data[PARAM_NOTIFICATION_TYPE]?.toInt()){
                NOTIFICATION_TYPE_CALL -> {
                    showCallNotification(
                        context,
                        data[PARAM_MESSAGE],
                        data[PARAM_CALL_ID]
                    )
                }
            }
        }
    }

    private fun showCallNotification(context: Context, message: String?, callId: String?) {
        if (ConnectycubeChatService.getInstance().isLoggedIn) return

        val notificationManager = NotificationManagerCompat.from(context)

        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createCallsNotificationsChannel(context, notificationManager)
        }

        val builder = NotificationCompat.Builder(context, CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.incoming_call))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(500))
            .setLights(context.resources.getColor(R.color.colorPrimary), 2000, 2000)
            .setColor(context.resources.getColor(R.color.colorPrimary))
            .setAutoCancel(true)

        notificationManager.notify(CALL_NOTIFICATION_ID, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCallsNotificationsChannel(context: Context, notificationManager: NotificationManagerCompat){
        val name = context.getString(R.string.calls_channel_name)
        val descriptionText = context.getString(R.string.calls_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CALLS_CHANNEL_ID, name, importance)
        channel.description = descriptionText
        channel.vibrationPattern = longArrayOf(500)
        channel.enableVibration(true)
        channel.lightColor = context.resources.getColor(R.color.colorPrimary)
        channel.enableLights(true)
        notificationManager.createNotificationChannel(channel)
    }
}