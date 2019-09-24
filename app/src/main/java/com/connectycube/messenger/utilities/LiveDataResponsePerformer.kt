package com.connectycube.messenger.utilities

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.core.server.Performer
import com.connectycube.messenger.api.ApiResponse
import com.connectycube.messenger.vo.AppExecutors
import java.util.concurrent.atomic.AtomicBoolean

open class LiveDataResponsePerformer<T, R> {
    val appExecutors = AppExecutors()

    open fun perform(performer: Performer<T>, converter: Converter<R, T>): MutableLiveData<ApiResponse<R>> {
        return object : MutableLiveData<ApiResponse<R>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    performer.performAsync(object : EntityCallback<T> {
                        override fun onSuccess(response: T, bundle: Bundle?) {
                            val wrapped = converter.convertTo(response)
                            postValue(ApiResponse.create(wrapped, bundle))
                        }

                        override fun onError(ex: ResponseException) {
                            postValue(ApiResponse.create(ex))
                        }
                    })
                }
            }
        }
    }

    open fun performSync(performer: Performer<T>,
                         converter: Converter<R, T>
    ): MutableLiveData<ApiResponse<R>> {
        return object : MutableLiveData<ApiResponse<R>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    appExecutors.networkIO().execute {
                        try {
                            val result = performer.perform()
                            val wrapped = converter.convertTo(result)
                            postValue(ApiResponse.create(wrapped, Bundle.EMPTY))
                        } catch (ex: ResponseException) {
                            postValue(ApiResponse.create(ex))
                        }
                    }
                }
            }
        }
    }

    open fun perform(performer: Performer<T>): MutableLiveData<ApiResponse<T>> {
        return object : MutableLiveData<ApiResponse<T>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    performer.performAsync(object : EntityCallback<T> {
                        override fun onSuccess(response: T, bundle: Bundle?) {
                            postValue(ApiResponse.create(response, bundle))
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