package com.connectycube.messenger.events

import com.connectycube.users.models.ConnectycubeUser


data class EventChatConnection (
    val connected: Boolean,
    val connectedUser: ConnectycubeUser?,
    val error: Exception?
){
    companion object {
        fun success(connectedUser: ConnectycubeUser): EventChatConnection {
            return EventChatConnection(true, connectedUser, null)
        }

        fun error(error: Exception): EventChatConnection {
            return EventChatConnection(false, null, error)
        }
    }
}