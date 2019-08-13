package com.connectycube.messenger.vo

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.connectycube.messenger.api.ApiEmptyResponse
import com.connectycube.messenger.api.ApiErrorResponse
import com.connectycube.messenger.api.ApiSuccessResponse

/**
 * A generic class that can provide a resource backed by both the sqlite database and the network.
 *
 *
 * You can read more about it in the [Architecture
 * Guide](https://developer.android.com/arch).
 * @param <ResultType>
 * @param <RequestType>
 */
abstract class DeleteNetworkBoundResource<ResultType, RequestType>
@MainThread constructor(private val appExecutors: AppExecutors) :
    BaseNetworkBoundResource<ResultType, RequestType>(appExecutors) {

    override fun process() {
        val apiResponse = createCall()
        addSource(apiResponse, Observer { response ->
            removeSource(apiResponse)
            when (response) {
                is ApiSuccessResponse -> {
                    appExecutors.diskIO().execute {
                        saveCallResult(processResponse(response))
                        processResponseBundle(response.bundle)
                        appExecutors.mainThread().execute {
                            setValue(Resource.success(null))
                        }
                    }
                }
                is ApiErrorResponse -> {
                    onRequestFailed()
                    setValue(Resource.error(response.errorMessage, null))
                }
            }
        })
    }

    @WorkerThread
    protected open fun processResponseBundle(bundle: Bundle?) {
    }

    protected open fun onRequestFailed() {}
}