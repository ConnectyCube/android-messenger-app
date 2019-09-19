package com.connectycube.messenger.events

import androidx.annotation.IntDef
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

const val EVENT_CHAT_LOGIN = 0
const val EVENT_INCOMING_MESSAGE = 1

object LiveDataBus {

    private val events = mutableMapOf<Int, Any>()


    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        EVENT_CHAT_LOGIN,
        EVENT_INCOMING_MESSAGE
    )
    internal annotation class EventIdentifier

    /**
     * Get the live data or create it if it's not already in memory.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> getLiveData(@EventIdentifier eventId: Int): EventLiveData<T> {
        return events.getOrPut(eventId, { EventLiveData<T>(eventId) }) as EventLiveData<T>
    }

    /**
     * Subscribe to the specified eventId and listen for updates on that eventId.
     */
    fun <T> subscribe(@EventIdentifier eventId: Int, lifecycle: LifecycleOwner,
                      action: Observer<T>
    ) {
        getLiveData<T>(eventId).observe(lifecycle, action)
    }

    /**
     * Removes this eventId when it has no observers.
     */
    fun unregister(@EventIdentifier eventId: Int) {
        events.remove(eventId)
    }

    /**
     * Publish an object to the specified eventId for all subscribers of that eventId.
     */
    fun <T> publish(@EventIdentifier eventId: Int, event: T) {
        getLiveData<T>(eventId).update(event)
    }
}