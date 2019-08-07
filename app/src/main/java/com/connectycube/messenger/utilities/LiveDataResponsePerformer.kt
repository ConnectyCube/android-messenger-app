package com.connectycube.messenger.utilities

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.core.server.Performer
import com.connectycube.messenger.api.ApiResponse
import java.util.concurrent.atomic.AtomicBoolean

open class LiveDataResponsePerformer<T, R> {

    open fun perform(performer: Performer<T>, converter: Converter<R, T>): MutableLiveData<ApiResponse<R>> {
        return object : MutableLiveData<ApiResponse<R>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    performer.performAsync(object : EntityCallback<T> {
                        override fun onSuccess(response: T, bundle: Bundle?) {
                            val wrapped = converter.convertTo(response)
                            postValue(ApiResponse.create(wrapped))
                        }

                        override fun onError(ex: ResponseException) {
                            postValue(ApiResponse.create(ex))
                        }
                    })
                }
            }
        }
    }
}