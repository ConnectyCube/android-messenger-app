package com.connectycube.messenger.utilities

import android.os.Bundle
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.core.server.Performer
import com.connectycube.messenger.api.ApiResponse

open class ResponsePerformer<T, R> {

    open fun perform(performer: Performer<T>, converter: Converter<R, T>, callback: Callback<R>?) {
        performer.performAsync(object : EntityCallback<T> {
            override fun onSuccess(response: T, bundle: Bundle?) {
                val wrapped = converter.convertTo(response)
                callback?.onResult(ApiResponse.create(wrapped, bundle))
            }

            override fun onError(ex: ResponseException) {
                callback?.onResult(ApiResponse.create(ex))
            }
        })
    }

    open fun perform(performer: Performer<T>, callback: Callback<T>?) {
        performer.performAsync(object : EntityCallback<T> {
            override fun onSuccess(response: T, bundle: Bundle?) {
                callback?.onResult(ApiResponse.create(response, bundle))
            }

            override fun onError(ex: ResponseException) {
                callback?.onResult(ApiResponse.create(ex))
            }
        })
    }

    interface Callback<T> {
        fun onResult(response: ApiResponse<T>)
    }
}