package com.connectycube.messenger.utilities

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.connectycube.messenger.api.ApiErrorResponse
import com.connectycube.messenger.api.ApiResponse
import com.connectycube.messenger.api.ApiSuccessResponse
import com.connectycube.messenger.vo.AppExecutors


abstract class UpdateResourceProcessor<RequestType>
@MainThread constructor(protected val appExecutors: AppExecutors) : ResponsePerformer.Callback<RequestType> {

    @MainThread
    override fun onResult(response: ApiResponse<RequestType>) {
        when (response) {
            is ApiSuccessResponse -> {
                appExecutors.diskIO().execute {
                    saveCallResult(response.body)
                }
            }
            is ApiErrorResponse -> {
                appExecutors.diskIO().execute {
                    processError(response.errorMessage)
                }
            }
        }
    }

    @WorkerThread
    abstract fun processError(errorMessage: String)

    @WorkerThread
    abstract fun saveCallResult(item: RequestType)
}