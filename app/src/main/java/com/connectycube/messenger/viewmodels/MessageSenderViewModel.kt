package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import com.connectycube.messenger.data.MessageSenderRepository
import com.connectycube.messenger.utilities.SingleLiveEvent
import com.connectycube.messenger.utilities.image.compressFileIfNeed
import com.connectycube.messenger.utilities.image.deleteImageCacheIfNeed
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeMessage

class MessageSenderViewModel internal constructor(
    val applicationContext: Application,
    private val dialog: ConnectycubeDialog,
    private val messageSenderRepo: MessageSenderRepository
) :
    AndroidViewModel(applicationContext) {

    val liveMessageSender = SingleLiveEvent<Resource<ConnectycubeMessage>>()
    val liveMessageAttachmentSender = MediatorLiveData<Resource<ConnectycubeMessage>>()

    fun sendAttachment(path: String, type: String, text: String) {
        val pathResized = compressFileIfNeed(applicationContext, path)
        val resource = messageSenderRepo.sendMessageAttachment(pathResized, type, text, dialog)
        liveMessageAttachmentSender.addSource(resource) { response ->
            liveMessageAttachmentSender.value = response
            when {
                response.status == Status.SUCCESS || response.status == Status.ERROR -> {
                    liveMessageAttachmentSender.removeSource(resource)
                    deleteImageCacheIfNeed(pathResized)
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