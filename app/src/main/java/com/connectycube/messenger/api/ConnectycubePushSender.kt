package com.connectycube.messenger.api

import com.connectycube.pushnotifications.models.ConnectycubePushEvent

class ConnectycubePushSender {
    private val service: ConnectycubeService = ConnectycubeService()

    fun sendCallPushEvent(event: ConnectycubePushEvent){
        service.createPushEvent(event, null)
    }
}