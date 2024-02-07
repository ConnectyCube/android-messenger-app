package com.connectycube.messenger.utilities

import com.connectycube.messenger.api.ApiResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.connectycube.core.utils.coroutineDispatcher

open class ResponsePerformer<T, R> {

    open fun perform(performer: suspend () -> T, converter: Converter<R, T>, callback: Callback<R>?) {

        GlobalScope.apply {
            launch(coroutineDispatcher) {
                try {
                    val wrapped = converter.convertTo(performer.invoke())
                    callback?.onResult(ApiResponse.create(wrapped, null))
                } catch (ex: Exception) {
                    callback?.onResult(ApiResponse.create(ex))
                }
            }
        }
    }

    open fun perform(performer: suspend () -> T, callback: Callback<T>?) {
        GlobalScope.apply {
            launch(coroutineDispatcher) {
                try {
                    val wrapped = performer.invoke()
                    callback?.onResult(ApiResponse.create(wrapped, null))
                } catch (ex: Exception) {
                    callback?.onResult(ApiResponse.create(ex))
                }
            }
        }
    }

    open fun performProgress(performer: suspend () -> T,
                     converter: Converter<R, T>,
                     callback: Callback<R>?
    ) {
        GlobalScope.apply {
            launch(coroutineDispatcher) {
                try {
                    val wrapped = converter.convertTo(performer.invoke())
                    callback?.onResult(ApiResponse.create(wrapped, null))
                } catch (ex: Exception) {
                    callback?.onResult(ApiResponse.create(ex))
                }
            }
        }
    }

    interface Callback<T> {
        fun onResult(response: ApiResponse<T>)
    }
}