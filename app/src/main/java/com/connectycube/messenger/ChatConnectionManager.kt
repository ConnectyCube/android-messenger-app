package com.connectycube.messenger

import android.content.Context
import android.os.Bundle
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.events.EVENT_CHAT_LOGIN
import com.connectycube.messenger.events.EventChatConnection
import com.connectycube.messenger.events.LiveDataBus
import com.connectycube.messenger.helpers.ProgressMarker
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.users.model.ConnectycubeUser
import timber.log.Timber

class ChatConnectionManager {

    companion object {
        @Volatile
        private var instance: ChatConnectionManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ChatConnectionManager().also { instance = it }
            }
    }

    private val  progressMarker = ProgressMarker()

    fun initWith(context: Context) {
        Timber.d("initWith, isPending ${progressMarker.isPending()}")
        if (progressMarker.isPending()) return

        if (SharedPreferencesManager.getInstance(context).currentUserExists()
            && !ConnectycubeChatService.getInstance().isLoggedIn
            && !progressMarker.isPending())
        {
            progressMarker.setPending(true)
            Timber.d("Start chat login")
            ConnectycubeChatService.getInstance().login(
                SharedPreferencesManager.getInstance(context).getCurrentUser(),
                object : EntityCallback<Void> {
                    override fun onSuccess(void: Void?, bundle: Bundle?) {
                        progressMarker.setPending(false)
                        Timber.d("Success login to chat, login = ${ConnectycubeChatService.getInstance().user.login}")
                        notifySuccessLoginToChat(ConnectycubeChatService.getInstance().user)
                        initCallManager(context)
                    }

                    override fun onError(ex: ResponseException) {
                        progressMarker.setPending(false)
                        Timber.d("Error while login to chat, error = ${ex.message}")
                        notifyErrorLoginToChat(ex)
                    }
                })
        }
    }

    private fun notifyErrorLoginToChat(exception: Exception) {
        LiveDataBus.publish(EVENT_CHAT_LOGIN, EventChatConnection.error(exception))
    }

    private fun notifySuccessLoginToChat(connectycubeUser: ConnectycubeUser) {
        LiveDataBus.publish(
            EVENT_CHAT_LOGIN,
            EventChatConnection.success(connectycubeUser)
        )
    }

    private fun initCallManager(context: Context) {
        RTCSessionManager.getInstance().init(context.applicationContext)
    }
}