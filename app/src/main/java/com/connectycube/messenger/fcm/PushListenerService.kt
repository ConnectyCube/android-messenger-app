package com.connectycube.messenger.fcm

import com.connectycube.messenger.helpers.AppNotificationManager
import com.connectycube.pushnotifications.services.fcm.FcmPushListenerService
import com.google.firebase.messaging.RemoteMessage

class PushListenerService: FcmPushListenerService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        AppNotificationManager.getInstance().processPushNotification(this, remoteMessage.data)
    }
}