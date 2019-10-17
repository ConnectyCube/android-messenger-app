package com.connectycube.messenger.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

class EventLiveData<T> (
    @LiveDataBus.EventIdentifier val id: Int
) : LiveData<T>() {

    fun update(toUpdateObject: T) {
        postValue(toUpdateObject)
    }

    override fun removeObserver(observer: Observer<in T>) {
        super.removeObserver(observer)
        if (!hasObservers()) {
            LiveDataBus.unregister(id)
        }
    }
}