package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.data.ChatMessageRepository
import com.connectycube.messenger.data.UserRepository

class ChatMessageListViewModelFactory(
    private val applicationContext: Application,
    private val chatRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
    private val chat: ConnectycubeChatDialog
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) =
        ChatMessageListViewModel(applicationContext, chatRepository, userRepository, chat) as T
}