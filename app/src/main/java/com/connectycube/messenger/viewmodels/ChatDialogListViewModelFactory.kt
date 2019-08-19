package com.connectycube.messenger.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.messenger.data.ChatRepository

class ChatDialogListViewModelFactory(
    private val repository: ChatRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = ChatDialogListViewModel(repository) as T
}