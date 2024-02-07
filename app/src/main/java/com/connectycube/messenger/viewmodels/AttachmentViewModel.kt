package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.connectycube.messenger.data.AttachmentRepository
import com.connectycube.messenger.vo.Resource
import com.connectycube.chat.models.ConnectycubeAttachment

class AttachmentViewModel internal constructor(
    applicationContext: Application,
    private val attachmentRepo: AttachmentRepository
) :
    AndroidViewModel(applicationContext) {

    fun uploadAttachment(path: String, type: String): LiveData<Resource<ConnectycubeAttachment>> =
        attachmentRepo.loadFileAsAttachment(path, type)

}