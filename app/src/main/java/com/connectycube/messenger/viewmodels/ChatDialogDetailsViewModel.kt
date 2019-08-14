package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.R
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository
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
                result.value = Resource.success(charDialog.conChat)
                result.removeSource(source)
            }
        }

        return result

    }

    fun isCurrentUser(user: ConnectycubeUser): Boolean {
        val currentUser = ConnectycubeChatService.getInstance().user
        return currentUser.login == user.login
    }

    fun getCurrentUser(): ConnectycubeUser {
        return ConnectycubeChatService.getInstance().user
    }
}