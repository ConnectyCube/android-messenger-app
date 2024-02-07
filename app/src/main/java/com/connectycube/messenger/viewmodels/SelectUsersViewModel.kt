package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.connectycube.messenger.R
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.vo.Resource
import com.connectycube.users.models.ConnectycubeUser

open class SelectUsersViewModel internal constructor(
    applicationContext: Application,
    private val usersRepository: UserRepository
) : AndroidViewModel(applicationContext) {

    internal var liveSelectedUsers = MutableLiveData<ArrayList<ConnectycubeUser>>()

    open fun getUsers(filterUsers: List<Int>): LiveData<Resource<List<ConnectycubeUser>>> {
        val result = MediatorLiveData<Resource<List<ConnectycubeUser>>>()
        result.value = Resource.loading(null)

        val source = usersRepository.getUsers()
        result.addSource(source) {
            if (it.isNullOrEmpty()) {
                result.value = Resource.error(
                    getApplication<Application>().getString(R.string.error_while_loading_users),
                    null)
            } else {
                result.value = Resource.success(it
                    .map { user -> user.conUser }
                    .filter { conUser -> !filterUsers.contains(conUser.id) })
                result.removeSource(source)
            }
        }

        return result
    }


    fun updateSelectedUsersStates() {
        liveSelectedUsers.postValue(liveSelectedUsers.value ?: arrayListOf())
    }

    fun updateUserSelection(user: ConnectycubeUser, isSelected: Boolean) {
        val selectedUsers: ArrayList<ConnectycubeUser> = liveSelectedUsers.value ?: arrayListOf()

        if (isSelected) selectedUsers.add(user) else selectedUsers.remove(user)
        liveSelectedUsers.postValue(selectedUsers)
    }
}