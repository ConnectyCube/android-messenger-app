package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.utils.DialogUtils
import com.connectycube.messenger.R
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser

class CreateChatDialogViewModel internal constructor(
    applicationContext: Application,
    private val usersRepository: UserRepository,
    private val chatRepository: ChatRepository
) : AndroidViewModel(applicationContext) {

    internal var liveSelectedUsers = MutableLiveData<ArrayList<ConnectycubeUser>>()

    fun getUsers(): LiveData<Resource<List<ConnectycubeUser>>> {
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
                    .filter { conUser -> !isCurrentUser(conUser) })
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

    private fun isCurrentUser(user: ConnectycubeUser): Boolean {
        val currentUser = SharedPreferencesManager.getInstance(getApplication()).getCurrentUser()
        return currentUser.login == user.login
    }

    fun createNewChatDialog(name: String? = null, avatar: String? = null): LiveData<Resource<ConnectycubeChatDialog>> {
        if (liveSelectedUsers.value == null) return MutableLiveData(
            Resource.error(
                getApplication<Application>().getString(R.string.select_users_choose_users),
                null
            )
        )

        val array = arrayOfNulls<ConnectycubeUser>(liveSelectedUsers.value!!.size)
        liveSelectedUsers.value?.toArray(array)
        val chatDialog: ConnectycubeChatDialog = DialogUtils.buildDialog(*array)
        name?.let{chatDialog.name = name}
        avatar?.let{chatDialog.photo = avatar}
        return Transformations.map(
            chatRepository.createChatDialog(
                Chat(
                    chatId = chatDialog.dialogId,
                    lastMessageDateSent = chatDialog.lastMessageDateSent,
                    createdAt = if (chatDialog.createdAt != null) chatDialog.createdAt.time else 0,
                    updatedAt = if (chatDialog.updatedAt != null) chatDialog.updatedAt.time else 0,
                    unreadMessageCount = if (chatDialog.unreadMessageCount != null) chatDialog.unreadMessageCount else 0,
                    name = chatDialog.name ?: (array[0]?.fullName ?: ""),
                    cubeChat = chatDialog
                )
            )
        ) {
            when {
                it.status == Status.SUCCESS -> Resource.success(it.data?.cubeChat)
                it.status == Status.ERROR -> it.message?.let { it1 -> Resource.error(it1, null) }
                else -> Resource.loading(null)
            }
        }
    }
}