package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.data.MessageSenderRepository
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status

class MessageSenderViewModel internal constructor(
    applicationContext: Application,
    private val dialog: ConnectycubeChatDialog,
    private val messageSenderRepo: MessageSenderRepository
) :
    AndroidViewModel(applicationContext) {

    val liveMessageSender = MediatorLiveData<Resource<ConnectycubeChatMessage>>()
    val liveMessageAttachmentSender = MediatorLiveData<Resource<ConnectycubeChatMessage>>()

    fun sendAttachment(path: String, type: String, text: String) {
        val resource = messageSenderRepo.sendMessageAttachment(path, type, text, dialog)
        liveMessageAttachmentSender.addSource(resource) { response ->
            liveMessageAttachmentSender.value = response
            when {
                response.status == Status.SUCCESS || response.status == Status.ERROR -> {
                    liveMessageAttachmentSender.removeSource(resource)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val resource = messageSenderRepo.sendMessageText(text, dialog)
        liveMessageSender.addSource(resource) { response ->
            liveMessageSender.removeSource(resource)
            liveMessageSender.value = response
        }
    }
}