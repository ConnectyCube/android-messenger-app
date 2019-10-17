package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.R
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.vo.Resource
import com.connectycube.users.model.ConnectycubeUser

class ChatDialogDetailsViewModel internal constructor(
    applicationContext: Application,
    private val dialogId: String,
    private val usersRepository: UserRepository,
    private val chatRepository: ChatRepository
) : AndroidViewModel(applicationContext) {

    val liveDialog by lazy {
        return@lazy getChatDialog(dialogId)
    }

    fun getUsers(chatDialog: ConnectycubeChatDialog): LiveData<Resource<List<ConnectycubeUser>>> {
        val result = MediatorLiveData<Resource<List<ConnectycubeUser>>>()
        result.value = Resource.loading(null)

        val source = usersRepository.getUsersByIds(*chatDialog.occupants.toIntArray())
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

    private fun getChatDialog(dialogId: String): LiveData<Resource<ConnectycubeChatDialog>> {
        val result = MediatorLiveData<Resource<ConnectycubeChatDialog>>()
        result.value = Resource.loading(null)

        val source = chatRepository.getChat(dialogId)
        result.addSource(source) { charDialog ->

            if (charDialog == null) {
                result.value = Resource.error(
                    getApplication<Application>().getString(R.string.something_went_wrong_try_again_later),
                    null
                )
            } else {
                result.value = Resource.success(charDialog.cubeChat)
            }
        }

        return result

    }

    fun isCurrentUser(user: ConnectycubeUser): Boolean {
        val currentUser = getCurrentUser()
        return currentUser.login == user.login
    }

    fun getCurrentUser(): ConnectycubeUser {
        return SharedPreferencesManager.getInstance(getApplication()).getCurrentUser()
    }

    fun updateGroupPhoto(dialogId: String, newPhoto: String) {
        liveDialog().postValue(Resource.loading(null))
        chatRepository.updateChatPhoto(dialogId, newPhoto, { error, chat ->
            liveDialog().postValue(Resource.error(error, chat.cubeChat))
        }, { progress -> liveDialog().postValue(Resource.loadingProgress(null, progress)) })
    }

    fun updateGroupDescription(dialogId: String, newDescription: String) {
        chatRepository.updateChatDescription(dialogId, newDescription) { error, chat ->
            liveDialog().postValue(Resource.error(error, chat.cubeChat))
        }
    }

    fun updateGroupName(dialogId: String, newName: String) {
        chatRepository.updateChatName(dialogId, newName) { error, chat ->
            liveDialog().postValue(Resource.error(error, chat.cubeChat))
        }
    }

    fun addUserToAdmins(dialogId: String, userId: Int) {
        chatRepository.addChatAdmins(dialogId, userId) { error, chat ->
            liveDialog().postValue(Resource.error(error, chat.cubeChat))
        }
    }

    fun removeUserFromAdmins(dialogId: String, userId: Int) {
        chatRepository.removeChatAdmins(dialogId, userId) { error, chat ->
            liveDialog().postValue(Resource.error(error, chat.cubeChat))
        }
    }


    fun addOccupants(dialogId: String, vararg usersIds: Int) {
        chatRepository.addChatOccupants(dialogId, *usersIds) { error, chat ->
            liveDialog().postValue(Resource.error(error, chat.cubeChat))
        }
    }

    fun removeOccupants(dialogId: String, vararg usersIds: Int) {
        chatRepository.removeChatOccupants(dialogId, *usersIds) { error, chat ->
            liveDialog().postValue(Resource.error(error, chat.cubeChat))
        }
    }

    private fun liveDialog(): MutableLiveData<Resource<ConnectycubeChatDialog>> {
        return liveDialog as MutableLiveData<Resource<ConnectycubeChatDialog>>
    }
}