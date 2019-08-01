package com.connectycube.messenger.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser

class CreateChatDialogViewModel internal constructor(
    private val userListViewModel: UserListViewModel
) : ViewModel() {

    private var liveSelectedUsers = MutableLiveData<ArrayList<ConnectycubeUser>>()

    fun getUsers(): LiveData<Resource<List<ConnectycubeUser>>> {
        return Transformations.map(userListViewModel.getUsers()){
            when {
                it.status == Status.SUCCESS -> Resource.success(it.data
                    ?.map { user -> user.conUser }
                    ?.filter { user -> !isCurrentUser(user) })
                it.status == Status.ERROR -> it.message?.let { it1 -> Resource.error(it1, null) }
                else -> Resource.loading(null)
            }
        }
    }

    fun updateUserSelection(user: ConnectycubeUser, isSelected: Boolean){
        val selectedUsers: ArrayList<ConnectycubeUser> = liveSelectedUsers.value ?: arrayListOf()

        if (isSelected) selectedUsers.add(user) else selectedUsers.remove(user)
        liveSelectedUsers.postValue(selectedUsers)
    }


    fun getLiveSelectedUsers(): MutableLiveData<ArrayList<ConnectycubeUser>> {
        return liveSelectedUsers
    }

    private fun isCurrentUser(user: ConnectycubeUser) :Boolean {
        val currentUser = ConnectycubeChatService.getInstance().user
        return currentUser.login == user.login
    }
}