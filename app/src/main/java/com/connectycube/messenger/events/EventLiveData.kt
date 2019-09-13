package com.connectycube.messenger.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

class EventLiveData (
    @LiveDataBus.EventIdentifier val id: Int
) : LiveData<Any>() {

    fun update(toUpdateObject: Any) {
        postValue(toUpdateObject)
    }

    override fun removeObserver(observer: Observer<in Any>) {
        super.removeObserver(observer)
        if (!hasObservers()) {
            LiveDataBus.unregister(id)
        }
    }
}