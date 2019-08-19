package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository

class ChatDialogDetailsViewModelFactory(
    private val applicationContext: Application,
    private val dialogId: String,
    private val usersRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) =
        ChatDialogDetailsViewModel(applicationContext, dialogId, usersRepository, chatRepository) as T
}