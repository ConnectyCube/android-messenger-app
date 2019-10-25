package com.connectycube.messenger.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.messenger.data.ChatMessageRepository
import com.connectycube.messenger.data.ChatRepository

class ChatDialogListViewModelFactory(
    private val chatRepository: ChatRepository,
    private val messageRepository: ChatMessageRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = ChatDialogListViewModel(chatRepository, messageRepository) as T
}