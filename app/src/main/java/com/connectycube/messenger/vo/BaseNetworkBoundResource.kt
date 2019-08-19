package com.connectycube.messenger.vo

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.connectycube.messenger.api.ApiResponse
import com.connectycube.messenger.api.ApiSuccessResponse

/**
 * A generic class that can provide a resource backed by both the sqlite database and the network.
 *
 *
 * You can read more about it in the [Architecture
 * Guide](https://developer.android.com/arch).
 * @param <ResultType>
 * @param <RequestType>
</RequestType></ResultType> */
abstract class BaseNetworkBoundResource<ResultType, RequestType>
@MainThread constructor(private val appExecutors: AppExecutors) {

    private val result = MediatorLiveData<Resource<ResultType>>()

    init {
        result.value = Resource.loading(null)
        @Suppress("LeakingThis")
        process()
    }

    abstract fun process()

    fun <S> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        result.addSource(source, onChanged)
    }

    protected fun<S> removeSource(liveData: LiveData<ApiResponse<S>>) {
        result.removeSource(liveData)
    }

    @MainThread
    protected fun setValue(newValue: Resource<ResultType>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    fun asLiveData() = result as LiveData<Resource<ResultType>>

    @WorkerThread
    protected open fun processResponse(response: ApiSuccessResponse<RequestType>) = response.body

    @WorkerThread
    protected abstract fun saveCallResult(item: RequestType)

    @MainThread
    protected abstract fun createCall(): LiveData<ApiResponse<RequestType>>
}