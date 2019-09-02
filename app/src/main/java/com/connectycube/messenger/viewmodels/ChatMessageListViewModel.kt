package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.map
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.R
import com.connectycube.messenger.data.ChatMessageRepository
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.utilities.convertToMessage
import com.connectycube.messenger.vo.Resource
import com.connectycube.users.model.ConnectycubeUser
import java.util.concurrent.atomic.AtomicBoolean

private const val PAGE_SIZE = 20

class ChatMessageListViewModel internal constructor(
    applicationContext: Application,
    private val repository: ChatMessageRepository,
    private val usersRepository: UserRepository,
    private val chat: ConnectycubeChatDialog
) :
    AndroidViewModel(applicationContext) {
    private val scrollAtomic = AtomicBoolean()
    var scroll: Boolean
        get() = scrollAtomic.getAndSet(false)
        set(value) {
            scrollAtomic.set(value)
        }
    var unreadCounter = 0
    private val dialogName = createShowDialog()
    private val repoResult = map(dialogName) {
        repository.postsOfDialogId(it.dialogId, PAGE_SIZE)
    }

    val networkState = Transformations.switchMap(repoResult, { it.networkState })
    val messages = Transformations.switchMap(repoResult, { it.pagedList })
    val refreshState = Transformations.switchMap(repoResult, { it.refreshState })

    private fun createShowDialog(): MutableLiveData<ConnectycubeChatDialog> {
        val dialogName = MutableLiveData<ConnectycubeChatDialog>()
        dialogName.value = chat
        return dialogName
    }

    fun getOccupants(chatDialog: ConnectycubeChatDialog): LiveData<Resource<List<ConnectycubeUser>>> {
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

    fun postItem(message: ConnectycubeChatMessage) {
        repository.insertItemIntoDb(convertToMessage(message))
    }

    fun updateItemSentStatus(messageId: String, userId: Int) {
        repository.updateItemSentStatus(messageId, userId)
    }

    fun updateItemReadStatus(messageId: String, userId: Int) {
        repository.updateItemReadStatus(messageId, userId)
    }

    fun updateItemDeliveredStatus(messageId: String, userId: Int) {
        repository.updateItemDeliveredStatus(messageId, userId)
    }

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun retry() {
        val listing = repoResult.value
        listing?.retry?.invoke()
    }
}
