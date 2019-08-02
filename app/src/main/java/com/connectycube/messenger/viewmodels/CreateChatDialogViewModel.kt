package com.connectycube.messenger.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.utils.DialogUtils
import com.connectycube.messenger.R
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.utilities.SAMPLE_CONFIG_FILE_NAME
import com.connectycube.messenger.utilities.getAllUsersFromFile
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser

class CreateChatDialogViewModel internal constructor(
    private val usersRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private var liveSelectedUsers = MutableLiveData<ArrayList<ConnectycubeUser>>()

    fun getUsers(context: Context): LiveData<Resource<List<ConnectycubeUser>>> {
        val users = getAllUsersFromFile(SAMPLE_CONFIG_FILE_NAME, context)
        val usersLogins: ArrayList<String> = ArrayList(users.map { user -> user.login })

        return Transformations.map(usersRepository.loadUsers(usersLogins)) {
            when {
                it.status == Status.SUCCESS -> Resource.success(it.data
                    ?.map { user -> user.conUser }
                    ?.filter { user -> !isCurrentUser(user) })
                it.status == Status.ERROR -> it.message?.let { it1 -> Resource.error(it1, null) }
                else -> Resource.loading(null)
            }
        }
    }

    fun updateUserSelection(user: ConnectycubeUser, isSelected: Boolean) {
        val selectedUsers: ArrayList<ConnectycubeUser> = liveSelectedUsers.value ?: arrayListOf()

        if (isSelected) selectedUsers.add(user) else selectedUsers.remove(user)
        liveSelectedUsers.postValue(selectedUsers)
    }


    fun getLiveSelectedUsers(): MutableLiveData<ArrayList<ConnectycubeUser>> {
        return liveSelectedUsers
    }

    private fun isCurrentUser(user: ConnectycubeUser): Boolean {
        val currentUser = ConnectycubeChatService.getInstance().user
        return currentUser.login == user.login
    }

    fun createNewChatDialog(context: Context): LiveData<Resource<ConnectycubeChatDialog>> {
        if (liveSelectedUsers.value == null) return MutableLiveData(
            Resource.error(
                context.getString(R.string.select_users_choose_users),
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