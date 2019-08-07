package com.connectycube.messenger.vo

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.api.ApiEmptyResponse
import com.connectycube.messenger.api.ApiErrorResponse
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
abstract class NetworkBoundResource<ResultType, RequestType>
@MainThread constructor(private val appExecutors: AppExecutors) {

    private val result = MediatorLiveData<Resource<ResultType>>()

    init {
        result.value = Resource.loading(null)
        @Suppress("LeakingThis")
        val dbSource = loadFromDb()
        result.addSource(dbSource) { data ->
            if(shouldShowMediateResult(data)) setValue(Resource.success(data))
            result.removeSource(dbSource)
            val apiResponse = createCallSlice()
            result.addSource(apiResponse) { response ->
                result.removeSource(apiResponse)
                var newCallData:RequestType? = null
                when (response) {
                    is ApiSuccessResponse -> {
                        newCallData = processResponse(response)
                    }
                }
            if (shouldFetch(data, newCallData)) {
                fetchFromNetwork(dbSource)
            } else {
                result.addSource(dbSource) { newData ->
                    setValue(Resource.success(newData))
                }
            }
        }
    }
}

@MainThread
private fun setValue(newValue: Resource<ResultType>) {
    if (result.value != newValue) {
        result.value = newValue
    }
}

private fun fetchFromNetwork(dbSource: LiveData<ResultType>) {
    val apiResponse = createCall()
    // we re-attach dbSource as a new source, it will dispatch its latest value quickly
    result.addSource(dbSource) { newData ->
        setValue(Resource.loading(newData))
    }
    result.addSource(apiResponse) { response ->
        result.removeSource(apiResponse)
        result.removeSource(dbSource)
        when (response) {
            is ApiSuccessResponse -> {
                appExecutors.diskIO().execute {
                    saveCallResult(processResponse(response))
                    appExecutors.mainThread().execute {
                        // we specially request a new live data,
                        // otherwise we will get immediately last cached value,
                        // which may not be updated with latest results received from network.
                        result.addSource(loadFromDb()) { newData ->
                            setValue(Resource.success(newData))
                        }
                    }
                }
            }
            is ApiEmptyResponse -> {
                appExecutors.mainThread().execute {
                    // reload from disk whatever we had
                    result.addSource(loadFromDb()) { newData ->
                        setValue(Resource.success(newData))
                    }
                }
            }
            is ApiErrorResponse -> {
                onFetchFailed()
                result.addSource(dbSource) { newData ->
                    setValue(Resource.error(response.errorMessage, newData))
                }
            }
        }
    }
}

protected open fun onFetchFailed() {}

fun asLiveData() = result as LiveData<Resource<ResultType>>

@WorkerThread
protected open fun processResponse(response: ApiSuccessResponse<RequestType>) = response.body

@WorkerThread
protected abstract fun saveCallResult(item: RequestType)

@MainThread
protected abstract fun shouldFetch(data: ResultType?, newData: RequestType?): Boolean

@MainThread
protected abstract fun loadFromDb(): LiveData<ResultType>

@MainThread
protected open fun shouldShowMediateResult(data: ResultType?): Boolean = false

@MainThread
protected abstract fun createCall(): LiveData<ApiResponse<RequestType>>

@MainThread
protected open fun createCallSlice(): LiveData<ApiResponse<RequestType>> =
    object : LiveData<ApiResponse<RequestType>>() {
        override fun onActive() {
            super.onActive()
            postValue(ApiResponse.create(NotImplementedError()))
        }
    }
}