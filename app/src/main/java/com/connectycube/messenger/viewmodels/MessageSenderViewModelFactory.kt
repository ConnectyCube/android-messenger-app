package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.messenger.data.MessageSenderRepository
import com.connectycube.chat.models.ConnectycubeDialog

class MessageSenderViewModelFactory(
    private val applicationContext: Application,
    private val dialog: ConnectycubeDialog,
    private val repository: MessageSenderRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) =
        MessageSenderViewModel(applicationContext, dialog, repository) as T
}