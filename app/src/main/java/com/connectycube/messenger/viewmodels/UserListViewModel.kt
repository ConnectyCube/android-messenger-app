package com.connectycube.messenger.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectycube.messenger.api.ApiResponse
import com.connectycube.messenger.data.User
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.vo.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class UserListViewModel internal constructor(val userRepository: UserRepository, val logins: ArrayList<String>) :
    ViewModel() {
    var userLiveData = MutableLiveData<Resource<List<User>>>()
    /**
     * Cancel all coroutines when the ViewModel is cleared.
     */
    @ExperimentalCoroutinesApi
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    fun getUsers(): LiveData<Resource<List<User>>>{
        return userRepository.loadUsers(logins)
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            userRepository.saveUser(user)
            userLiveData.postValue(Resource.success(arrayListOf(user)))
        }
    }
}