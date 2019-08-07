package com.connectycube.messenger.vo

import com.connectycube.messenger.vo.Status.SUCCESS
import com.connectycube.messenger.vo.Status.ERROR
import com.connectycube.messenger.vo.Status.LOADING
/**
 * A generic class that holds a value with its loading status.
 * @param <T>
</T> */
data class Resource<out T>(val status: Status, val data: T?, val message: String?, val progress: Int?) {

    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(SUCCESS, data, null, null)
        }

        fun <T> error(msg: String, data: T?): Resource<T> {
            return Resource(ERROR, data, msg, null)
        }

        fun <T> loading(data: T?): Resource<T> {
            return Resource(LOADING, data, null, null)
        }

        fun <T> loadingProgress(data: T?, progress: Int): Resource<T> {
            return Resource(LOADING, data, null, progress)
        }
    }
}