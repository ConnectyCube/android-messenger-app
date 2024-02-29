package com.connectycube.messenger.fcm

import com.connectycube.core.ConnectycubeSessionManager
import com.connectycube.messenger.api.PushService
import com.connectycube.messenger.helpers.AppNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class PushListenerService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Timber.d("Refreshed token: $token")

        if (ConnectycubeSessionManager.activeSession?.user?.id.takeIf { it != 0 } != null) {
            //user is signed in need to createPushSubscription with new token
            PushService.instance.subscribeToPushes(this, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        //need to process push notification
        Timber.d("From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("remoteMessage.data ${remoteMessage.data}")
            AppNotificationManager.getInstance().processPushNotification(this, remoteMessage.data)
        }
    }
}