package com.connectycube.messenger

import androidx.lifecycle.*
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.messenger.events.EVENT_CHAT_LOGIN
import com.connectycube.messenger.events.EventChatConnection
import com.connectycube.messenger.events.LiveDataBus
import timber.log.Timber


class ChatAppLifecycleObserver : DefaultLifecycleObserver {

    fun registeredObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun registeredEventChat(lifecycle: LifecycleOwner) {
        LiveDataBus.subscribe(EVENT_CHAT_LOGIN, lifecycle, Observer<EventChatConnection> {
            if (it.connected) {
                Timber.d("connected")
                if (ProcessLifecycleOwner.get().lifecycle.currentState == Lifecycle.State.RESUMED) {
                    Timber.d("connected enterActiveState")
                    ConnectycubeChatService.getInstance().enterActiveState()
                }
            }
        })
    }

    private fun unregisteredEventChat(lifecycle: LifecycleOwner) {
        LiveDataBus.unsubscribe<EventChatConnection>(EVENT_CHAT_LOGIN, lifecycle)
    }

    fun unregisteredObserver() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        registeredEventChat(owner)
        if (ConnectycubeChatService.getInstance().isLoggedIn) {
            Timber.d("onStart enterActiveState")
            ConnectycubeChatService.getInstance().enterActiveState()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        unregisteredEventChat(owner)
        if (ConnectycubeChatService.getInstance().isLoggedIn) {
            Timber.d("onStop enterInactiveState")
            ConnectycubeChatService.getInstance().enterInactiveState()
        }
    }
}