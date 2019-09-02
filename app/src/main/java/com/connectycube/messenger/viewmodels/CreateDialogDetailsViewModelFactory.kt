package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.messenger.data.AvatarRepository

class CreateDialogDetailsViewModelFactory(
    private val applicationContext: Application,
    private val avatarRepository: AvatarRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) =
        CreateDialogDetailsViewModel(applicationContext, avatarRepository) as T
}