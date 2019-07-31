package com.connectycube.messenger.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.data.ChatMessageRepository

class MessageListViewModelFactory(
    private val repository: ChatMessageRepository,
    private val chat: ConnectycubeChatDialog
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = ChatMessageViewModel(repository, chat) as T
}