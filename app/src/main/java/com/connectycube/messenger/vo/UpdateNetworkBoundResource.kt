package com.connectycube.messenger.vo

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
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
abstract class UpdateNetworkBoundResource<ResultType, RequestType>
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
                        appExecutors.mainThread().execute {
                            addSource(loadFromDb(), Observer { newData ->
                                setValue(Resource.success(newData))
                            })
                        }
                    }
                }
                is ApiErrorResponse -> {
                    appExecutors.mainThread().execute {
                        addSource(loadFromDb(), Observer { oldData ->
                            setValue(Resource.error(response.errorMessage, oldData))
                        })
                    }
                }
                else -> {}
            }
        })
    }

    @MainThread
    protected abstract fun loadFromDb(): LiveData<ResultType>

}