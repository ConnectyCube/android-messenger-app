package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.utils.DialogUtils
import com.connectycube.messenger.R
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser

class CreateChatDialogViewModel internal constructor(
    applicationContext: Application,
    private val usersRepository: UserRepository,
    private val chatRepository: ChatRepository
) : AndroidViewModel(applicationContext) {

    internal var liveSelectedUsers = MutableLiveData<ArrayList<ConnectycubeUser>>()

    fun getUsers(): LiveData<List<ConnectycubeUser>> {
        return Transformations.map(usersRepository.getUsers()) {
            it.map { user -> user.conUser }.filter { user -> !isCurrentUser(user) }
        }
    }

    fun updateUserSelection(user: ConnectycubeUser, isSelected: Boolean) {
        val selectedUsers: ArrayList<ConnectycubeUser> = liveSelectedUsers.value ?: arrayListOf()

        if (isSelected) selectedUsers.add(user) else selectedUsers.remove(user)
        liveSelectedUsers.postValue(selectedUsers)
    }

    private fun isCurrentUser(user: ConnectycubeUser): Boolean {
        val currentUser = ConnectycubeChatService.getInstance().user
        return currentUser.login == user.login
    }

    fun createNewChatDialog(): LiveData<Resource<ConnectycubeChatDialog>> {
        if (liveSelectedUsers.value == null) return MutableLiveData(
            Resource.error(
                getApplication<Application>().getString(R.string.select_users_choose_users),
                null
            )
        )

        val array = arrayOfNulls<ConnectycubeUser>(liveSelectedUsers.value!!.size)
        liveSelectedUsers.value?.toArray(array)
        val chatDialog: ConnectycubeChatDialog = DialogUtils.buildDialog(*array)
        return Transformations.map(
            chatRepository.createChatDialog(
                Chat(
                    chatId = chatDialog.dialogId,
                    lastMessageDateSent = chatDialog.lastMessageDateSent,
                    name = chatDialog.name ?: (array[0]?.fullName ?: ""),
                    conChat = chatDialog
                )
            )
        ) {
            when {
                it.status == Status.SUCCESS -> Resource.success(it.data?.conChat)
                it.status == Status.ERROR -> it.message?.let { it1 -> Resource.error(it1, null) }
                else -> Resource.loading(null)
            }
        }
    }
}