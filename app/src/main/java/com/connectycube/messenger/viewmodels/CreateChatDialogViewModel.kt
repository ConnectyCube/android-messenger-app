package com.connectycube.messenger.viewmodels

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.*
import com.connectycube.messenger.R
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.utilities.convertToChat
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeDialogType
import com.connectycube.users.models.ConnectycubeUser

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
                    null
                )
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

    fun createNewChatDialog(name: String? = null, avatar: String? = null): LiveData<Resource<ConnectycubeDialog>> {
        if (liveSelectedUsers.value == null) return MutableLiveData(
            Resource.error(
                getApplication<Application>().getString(R.string.select_users_choose_users),
                null
            )
        )

        val array = arrayOfNulls<ConnectycubeUser>(liveSelectedUsers.value!!.size)
        liveSelectedUsers.value?.toArray(array)
        val chatDialog: ConnectycubeDialog = buildDialog(*array)!!
        name?.let { chatDialog.name = name }
        avatar?.let { chatDialog.photo = avatar }
        return Transformations.map(
            chatRepository.createChatDialog(
                convertToChat(chatDialog).apply {
                    this.name = chatDialog.name ?: (array[0]?.fullName ?: "")
                }
            )
        ) {
            when {
                it.status == Status.SUCCESS -> Resource.success(it.data)
                it.status == Status.ERROR -> it.message?.let { it1 -> Resource.error(it1, null) }
                else -> Resource.loading(null)
            }
        }
    }

    private fun buildDialog(vararg users: ConnectycubeUser?): ConnectycubeDialog? {
        return if (users.isEmpty()) {
            throw IllegalArgumentException("Users array can't be empty")
        } else {
            when {
                users.size > 1 -> {
                    ConnectycubeDialog(dialogId = "", name = createChatNameFromUserList(*users), type = ConnectycubeDialogType.GROUP, occupantsIds = users.map { it!!.id }.toCollection(ArrayList()))
                }
                users.size == 1 -> {
                    ConnectycubeDialog(dialogId = "", name = null, type = ConnectycubeDialogType.PRIVATE, occupantsIds = users.map { it!!.id }.toCollection(ArrayList()))
                }
                else -> null
            }
        }
    }

    private fun createChatNameFromUserList(vararg users: ConnectycubeUser?): String {
        var chatName = ""
        for (user in users) {
            val prefix = if (chatName == "") "" else ", "
            val fullName: String =
                if (!TextUtils.isEmpty(user!!.fullName)) user.fullName!! else user.id.toString()
            chatName = chatName + prefix + fullName
        }
        return chatName
    }
}