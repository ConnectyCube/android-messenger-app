package com.connectycube.messenger.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.vo.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ChatListViewModel internal constructor(val chatRepository: ChatRepository) :
    ViewModel() {
    var chatLiveData = MutableLiveData<Resource<List<Chat>>>()
    /**
     * Cancel all coroutines when the ViewModel is cleared.
     */
    @ExperimentalCoroutinesApi
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    fun getChats(): LiveData<Resource<List<Chat>>> {
        return chatRepository.loadChats()
    }

    fun updateChat(chat: Chat) {
        viewModelScope.launch {
            chatRepository.saveChat(chat)
            chatLiveData.postValue(Resource.success(arrayListOf(chat)))
        }
    }
}