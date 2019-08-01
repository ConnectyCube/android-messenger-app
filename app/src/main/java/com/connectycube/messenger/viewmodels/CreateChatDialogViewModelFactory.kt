package com.connectycube.messenger.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CreateChatDialogViewModelFactory(
    private val userListViewModel: UserListViewModel
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = CreateChatDialogViewModel(userListViewModel) as T
}