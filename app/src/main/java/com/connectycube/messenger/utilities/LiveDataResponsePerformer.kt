package com.connectycube.messenger.utilities

import androidx.lifecycle.MutableLiveData
import com.connectycube.messenger.api.ApiResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.connectycube.core.utils.coroutineDispatcher
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

open class LiveDataResponsePerformer<T, R> {

    open fun perform(performer: suspend () -> T, converter: Converter<R, T>): MutableLiveData<ApiResponse<R>> {
        return object : MutableLiveData<ApiResponse<R>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    GlobalScope.apply {
                        launch(coroutineDispatcher) {
                            try {
                                val wrapped = converter.convertTo(performer.invoke())
                                postValue(ApiResponse.create(wrapped, null))
                            } catch (ex: Exception) {
                                Timber.d("loadFileAsAttachment Exception= $ex")
                                postValue(ApiResponse.create(ex))
                            }
                        }
                    }
                }
            }
        }
    }

    open fun perform(performer: suspend () -> T): MutableLiveData<ApiResponse<T>> {
        return object : MutableLiveData<ApiResponse<T>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    GlobalScope.apply {
                        launch(coroutineDispatcher) {
                            try {
                                val wrapped = performer.invoke()
                                postValue(ApiResponse.create(wrapped, null))
                            } catch (ex: Exception) {
                                postValue(ApiResponse.create(ex))
                            }
                        }
                    }
                }
            }
        }
    }
}