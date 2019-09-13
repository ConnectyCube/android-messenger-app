package com.connectycube.messenger.events

import android.util.SparseArray
import androidx.annotation.IntDef
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

const val EVENT_CHAT_LOGIN = 0
const val EVENT_INCOMING_MESSAGE = 1

object LiveDataBus {

    private val events = SparseArray<EventLiveData>()



    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        EVENT_CHAT_LOGIN,
        EVENT_INCOMING_MESSAGE
    )
    internal annotation class EventIdentifier

    /**
     * Get the live data or create it if it's not already in memory.
     */
    private fun getLiveData(@EventIdentifier eventId: Int): EventLiveData {
        var liveData: EventLiveData? = events.get(eventId)
        if (liveData == null) {
            liveData = EventLiveData(eventId)
            events.put(eventId, liveData)
        }

        return liveData
    }

    /**
     * Subscribe to the specified eventId and listen for updates on that eventId.
     */
    fun subscribe(@EventIdentifier eventId: Int, lifecycle: LifecycleOwner, action: Observer<Any>) {
        getLiveData(eventId).observe(lifecycle, action)
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
    fun publish(@EventIdentifier eventId: Int, event: Any) {
        getLiveData(eventId).update(event)
    }
}