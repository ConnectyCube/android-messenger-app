package com.connectycube.messenger.api

import com.connectycube.pushnotifications.model.ConnectycubeEvent

class ConnectycubePushSender {
    private val service: ConnectycubeService = ConnectycubeService()

    fun sendCallPushEvent(event: ConnectycubeEvent){
        service.createPushEvent(event, null)
    }
}