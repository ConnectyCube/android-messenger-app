package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.connectycube.messenger.R
import com.connectycube.messenger.data.User
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status
import com.connectycube.users.models.ConnectycubeUser

class UserDetailsViewModel internal constructor(
    applicationContext: Application,
    private val userId: Int,
    private val userRepository: UserRepository
) : AndroidViewModel(applicationContext) {

    val liveDataUser by lazy {
        return@lazy getUser(userId)
    }

    private fun getUser(userId: Int): LiveData<Resource<ConnectycubeUser>> {
        val result = MediatorLiveData<Resource<ConnectycubeUser>>()
        result.value = Resource.loading(null)

        val source = userRepository.getUser(userId)
        result.addSource(source) {
            if (it == null) {
                result.value = Resource.error(
                    getApplication<Application>().getString(R.string.error_while_loading_users),
                    null
                )
            } else {
                result.value = Resource.success(it.conUser)
                result.removeSource(source)
            }
        }
        return result
    }

    fun updateNameLiveData(newName: String): LiveData<Resource<User>> {
        return userRepository.updateUserName(userId, newName)
    }

    fun updateName(newName: String) {
        val source = userRepository.updateUserName(userId, newName)
        liveDataUser().addSource(source) {
            if (it == null) {
                liveDataUser().value = Resource.error(
                    getApplication<Application>().getString(R.string.error_while_loading_users),
                    null
                )
            } else if (it.status == Status.SUCCESS) {
                liveDataUser().value = Resource.success(it.data?.conUser)
                liveDataUser().removeSource(source)
            }
        }
    }

    fun updateAvatar(newAvatar: String) {
        val source = userRepository.updateUserAvatar(userId, newAvatar)
        liveDataUser().addSource(source) {
            if (it.status == Status.SUCCESS) {
                liveDataUser().value = Resource.success(it.data)
                liveDataUser().removeSource(source)
            } else {
                liveDataUser().value = it
            }
        }
    }


    private fun liveDataUser(): MediatorLiveData<Resource<ConnectycubeUser>> {
        return liveDataUser as MediatorLiveData<Resource<ConnectycubeUser>>
    }

}