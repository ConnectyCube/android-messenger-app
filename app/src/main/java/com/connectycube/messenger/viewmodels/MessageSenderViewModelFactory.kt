package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.data.MessageSenderRepository

class MessageSenderViewModelFactory(
    private val applicationContext: Application,
    private val dialog: ConnectycubeChatDialog,
    private val repository: MessageSenderRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) =
        MessageSenderViewModel(applicationContext, dialog, repository) as T
}