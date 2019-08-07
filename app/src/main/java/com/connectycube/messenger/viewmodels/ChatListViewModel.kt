package com.connectycube.messenger.viewmodels

import androidx.lifecycle.*
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.vo.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ChatListViewModel internal constructor(val chatRepository: ChatRepository) :
    ViewModel() {
    var chatLiveData = MutableLiveData<Resource<List<Chat>>>()
    val chatMediatorLiveData = MediatorLiveData<Resource<List<Chat>>>()
    /**
     * Cancel all coroutines when the ViewModel is cleared.
     */
    @ExperimentalCoroutinesApi
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    fun getChats(): LiveData<Resource<List<Chat>>> {
        val chatListLiveData = chatRepository.loadChats()
        chatMediatorLiveData.addSource(chatListLiveData){data ->
            chatMediatorLiveData.value = data
        }
        return chatMediatorLiveData
    }

    fun updateChat(chat: Chat) {
        viewModelScope.launch {
            chatRepository.saveChat(chat)
            chatLiveData.postValue(Resource.success(arrayListOf(chat)))
        }
    }

    fun updateChat(dialogId: String) {
        chatMediatorLiveData.addSource(chatRepository.update(dialogId)){data ->
            chatMediatorLiveData.value = data
        }
    }
}