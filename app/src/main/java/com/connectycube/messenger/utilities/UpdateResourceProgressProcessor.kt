package com.connectycube.messenger.utilities

import androidx.annotation.WorkerThread
import com.connectycube.messenger.api.ApiProgressResponse
import com.connectycube.messenger.api.ApiResponse
import com.connectycube.messenger.vo.AppExecutors

abstract class UpdateResourceProgressProcessor<RequestType>(appExecutors: AppExecutors) :
    UpdateResourceProcessor<RequestType>(appExecutors) {

    override fun onResult(response: ApiResponse<RequestType>) {
        when (response) {
            is ApiProgressResponse -> {
                appExecutors.diskIO().execute {
                    processProgress(response.progress)
                }
            }
            else -> super.onResult(response)
        }
    }

    @WorkerThread
    abstract fun processProgress(progress: Int)
}