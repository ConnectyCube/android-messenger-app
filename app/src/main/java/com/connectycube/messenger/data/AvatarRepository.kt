package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.messenger.api.*
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.Resource

class AvatarRepository private constructor(private val appExecutors: AppExecutors) {
    private val service: ConnectycubeService = ConnectycubeService()

    fun uploadFile(
        path: String
    ): LiveData<Resource<String>> {
        val result = MediatorLiveData<Resource<String>>()
        result.value = Resource.loading(null)

        val apiResponse = service.uploadAvatar(path)
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
        private var instance: AvatarRepository? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: AvatarRepository(AppExecutors()).also { instance = it }
            }
    }
}