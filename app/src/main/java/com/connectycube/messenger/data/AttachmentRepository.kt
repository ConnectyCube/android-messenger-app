package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.messenger.api.ApiEmptyResponse
import com.connectycube.messenger.api.ApiErrorResponse
import com.connectycube.messenger.api.ApiSuccessResponse
import com.connectycube.messenger.api.ConnectycubeService
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.Resource

class AttachmentRepository private constructor(private val appExecutors: AppExecutors) {
    private val service: ConnectycubeService = ConnectycubeService()

    fun loadFileAsAttachment(path: String): LiveData<Resource<ConnectycubeAttachment>> {
        val result = MediatorLiveData<Resource<ConnectycubeAttachment>>()
        result.value = Resource.loading(null)

        val apiResponse = service.loadFileAsAttachment(path)
        result.addSource(apiResponse) { response ->
            result.removeSource(apiResponse)
            when (response) {
                is ApiSuccessResponse -> {
                    result.value = Resource.success(response.body)
                }
                is ApiEmptyResponse -> {
                    result.value = Resource.success(null)
                }
                is ApiErrorResponse -> {
                    result.value = Resource.error(response.errorMessage, null)
                }
            }
        }
        return result
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AttachmentRepository? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: AttachmentRepository(AppExecutors()).also { instance = it }
            }
    }
}