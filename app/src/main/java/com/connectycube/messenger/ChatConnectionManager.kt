package com.connectycube.messenger

import android.content.Context
import com.connectycube.messenger.events.EVENT_CHAT_LOGIN
import com.connectycube.messenger.events.EventChatConnection
import com.connectycube.messenger.events.LiveDataBus
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.ConnectyCube
import com.connectycube.chat.realtime.ConnectycubeConnectionListener
import com.connectycube.users.models.ConnectycubeUser
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ChatConnectionManager {

    companion object {
        @Volatile
        private var instance: ChatConnectionManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ChatConnectionManager().also { instance = it }
            }
    }

    private val isPending = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    private val chatAppObserver = ChatAppLifecycleObserver()

    fun initWith(context: Context) {
        Timber.d("initWith, isPending ${isPending.get()}")
        if (isPending.get() || isInitialized.get()) return

        if (SharedPreferencesManager.getInstance(context).currentUserExists()
            && !ConnectyCube.chat.isLoggedIn()
        ) {
            isPending.set(true)
            Timber.d("Start chat login")
            initConnectionListener()
            ConnectyCube.chat.login(
                SharedPreferencesManager.getInstance(context).getCurrentUser(),
                {
                    isPending.set(false)
                    isInitialized.set(true)
                    registerAppLifeCycleObserver()
                    initCallManager(context)
                },
                { ex ->
                    isPending.set(false)
                    Timber.d("Error while login to chat, error = ${ex.message}")
                    notifyErrorLoginToChat(ex)
                })
        }
    }

    private fun initConnectionListener() {
        ConnectyCube.chat.addConnectionListener(object : ConnectycubeConnectionListener {
            override fun onConnected() {
                Timber.d("authenticated")
                notifySuccessLoginToChat(ConnectyCube.chat.userForLogin!!)
            }

            override fun onDisconnected() {
                Timber.d("onDisconnected")
                notifyErrorLoginToChat(Exception("onDisconnected"))
            }
        })
    }

    private fun registerAppLifeCycleObserver() {
        chatAppObserver.registeredObserver()
    }

    private fun unregisterAppLifeCycleObserver() {
        chatAppObserver.unregisteredObserver()
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

    fun terminate() {
        ConnectyCube.chat.logout()
        unregisterAppLifeCycleObserver()
        isPending.set(false)
        isInitialized.set(false)
        instance = null
    }
}