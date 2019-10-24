package com.connectycube.messenger.viewmodels

import androidx.lifecycle.*
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.ChatMessageRepository
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ChatDialogListViewModel internal constructor(val chatRepository: ChatRepository,
                                                   val messageRepository: ChatMessageRepository) :
    ViewModel() {
    var chatLiveData = MutableLiveData<Resource<List<Chat>>>()

    val chatLiveDataLazy by lazy {
        return@lazy getChatDialogs()
    }
    /**
     * Cancel all coroutines when the ViewModel is cleared.
     */
    @ExperimentalCoroutinesApi
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    private fun getChats(): LiveData<Resource<List<Chat>>> {
        return chatRepository.loadChats()
    }

    fun getChatDialogs(): LiveData<Resource<List<ConnectycubeChatDialog>>>{
        return transformData(getChats())
    }

    fun updateChat(chat: Chat) {
        viewModelScope.launch {
            chatRepository.saveChat(chat)
            chatLiveData.postValue(Resource.success(arrayListOf(chat)))
        }
    }

    fun updateChat(dialogId: String) {
        chatRepository.updateChat(dialogId)
    }

    fun deleteChat(chatDialog: ConnectycubeChatDialog): LiveData<Resource<List<ConnectycubeChatDialog>>>{
        return transformData(chatRepository.deleteChats(false, chatsIds = *arrayOf(chatDialog.dialogId)))
    }

    fun updateMessageReadStatus(messageId: String, userId: Int) {
        messageRepository.updateItemReadStatus(messageId, userId)
    }

    fun updateMessageDeliveredStatus(messageId: String, userId: Int) {
        messageRepository.updateItemDeliveredStatus(messageId, userId)
    }

    private fun transformData(source: LiveData<Resource<List<Chat>>>): LiveData<Resource<List<ConnectycubeChatDialog>>>{
        return Transformations.map(source){
            when(it.status){
                Status.LOADING -> Resource.loading(null)
                Status.SUCCESS -> Resource.success(it.data)
                Status.ERROR -> Resource.error(it.message.toString(), null)
            }
        }
    }
}