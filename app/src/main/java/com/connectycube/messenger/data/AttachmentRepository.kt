package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.messenger.api.*
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.Resource
import com.connectycube.chat.models.ConnectycubeAttachment

class AttachmentRepository private constructor(private val appExecutors: AppExecutors) {
    private val service: ConnectycubeService = ConnectycubeService()

    fun loadFileAsAttachment(path: String, type: String): LiveData<Resource<ConnectycubeAttachment>> {
        val result = MediatorLiveData<Resource<ConnectycubeAttachment>>()
        result.value = Resource.loading(null)

        val apiResponse = service.loadFileAsAttachment(path, type)
        result.addSource(apiResponse) { response ->
            when (response) {
                is ApiSuccessResponse -> {
                    result.value = Resource.success(response.body)
                }
                is ApiEmptyResponse -> {
                    result.value = Resource.success(null)
                }
                is ApiProgressResponse -> {
                    result.value = Resource.loadingProgress(null, response.progress)
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