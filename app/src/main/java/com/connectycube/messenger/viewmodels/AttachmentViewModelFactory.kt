package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.messenger.data.AttachmentRepository

class AttachmentViewModelFactory(
    private val applicationContext: Application,
    private val attachmentRepository: AttachmentRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = AttachmentViewModel(applicationContext, attachmentRepository) as T
}