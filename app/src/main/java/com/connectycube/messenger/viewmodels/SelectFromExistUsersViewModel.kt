package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.messenger.R
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.vo.Resource
import com.connectycube.users.models.ConnectycubeUser

class SelectFromExistUsersViewModel internal constructor(
    applicationContext: Application,
    private val usersRepository: UserRepository
) : SelectUsersViewModel (applicationContext, usersRepository) {

    fun getUsers(allOccupants: ArrayList<Int>): LiveData<Resource<List<ConnectycubeUser>>> {
        val result = MediatorLiveData<Resource<List<ConnectycubeUser>>>()
        result.value = Resource.loading(null)

        val source = usersRepository.getUsersByIds(*allOccupants.filter { it != getCurrentUser().id }.toIntArray())
        result.addSource(source) {
            if (it.isNullOrEmpty()) {
                result.value = Resource.error(
                    getApplication<Application>().getString(R.string.error_while_loading_users),
                    null
                )
            } else {
                result.value = Resource.success(it
                    .map { user -> user.conUser })
                result.removeSource(source)
            }
        }

        return result
    }

    private fun getCurrentUser(): ConnectycubeUser {
        return SharedPreferencesManager.getInstance(getApplication()).getCurrentUser()
    }
}