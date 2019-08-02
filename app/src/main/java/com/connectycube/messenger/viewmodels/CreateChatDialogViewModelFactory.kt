package com.connectycube.messenger.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository

class CreateChatDialogViewModelFactory(
    private val usersRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) =
        CreateChatDialogViewModel(usersRepository, chatRepository) as T
}