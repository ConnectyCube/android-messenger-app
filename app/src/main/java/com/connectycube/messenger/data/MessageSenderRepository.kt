package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.messenger.api.*
import com.connectycube.messenger.utilities.convertToListAttachment
import com.connectycube.messenger.utilities.convertToMessage
import com.connectycube.messenger.utilities.getImageSize
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.Resource
import com.connectycube.ConnectyCube
import com.connectycube.chat.models.*
import timber.log.Timber

class MessageSenderRepository private constructor(private val messageDao: MessageDao,
                                                  private val attachmentDao: AttachmentDao,
                                                  private val appExecutors: AppExecutors
) {
    private val service: ConnectycubeService = ConnectycubeService()

    fun sendMessageAttachment(path: String,
                              type: String,
                              text: String,
                              dialog: ConnectycubeDialog
    ): LiveData<Resource<ConnectycubeMessage>> {
        val messageToTempSave = createMessage(dialog, text)
        val attachmentToTempSave = createAttachment(path, type)
        messageToTempSave.attachments!!.add(attachmentToTempSave)
        saveMediatorResult(messageToTempSave)

        val result = MediatorLiveData<Resource<ConnectycubeMessage>>()
        result.value = Resource.loading(null)

        val apiResponse = service.loadFileAsAttachment(path, type)
        result.addSource(apiResponse) { response ->
            when (response) {
                is ApiEmptyResponse -> {
                    result.value = Resource.success(null)
                }
                is ApiProgressResponse -> {
                    result.value = Resource.loadingProgress(messageToTempSave, response.progress)
                }
                is ApiErrorResponse -> {
                    deleteTempMessage(messageToTempSave.messageId!!, path.hashCode().toString())
                    result.value = Resource.error(response.errorMessage, null)
                    result.removeSource(apiResponse)
                }
                is ApiSuccessResponse -> {
                    val attachment = response.body
                    updateAttachmentSize(attachmentToTempSave, attachment)

                    val messageUpdated = buildMessage(messageToTempSave, attachment, dialog)
                    result.removeSource(apiResponse)

                    val apiSenderResponse = sendMessage(messageUpdated, dialog)
                    result.addSource(apiSenderResponse) {
                        result.removeSource(apiSenderResponse)
                        result.value = it
                    }

                }
            }
        }
        return result
    }

    private fun deleteTempMessage(messageId: String, attachId: String? = null) {
        appExecutors.diskIO().execute {
            attachId?.let { attachmentDao.deleteById(attachId) }
            messageDao.deleteByMessageId(messageId)
        }
    }

    fun sendMessageText(text: String,
                        dialog: ConnectycubeDialog
    ): LiveData<Resource<ConnectycubeMessage>> {
        val messageToTempSave = createMessage(dialog, text)
        saveMediatorResult(messageToTempSave)

        val result = MediatorLiveData<Resource<ConnectycubeMessage>>()
        val messageUpdated = buildMessage(messageToTempSave, dialog = dialog)
        val apiSenderResponse = sendMessage(messageUpdated, dialog)
        result.addSource(apiSenderResponse) {
            result.removeSource(apiSenderResponse)
            result.value = it
        }
        return result
    }

    private fun saveMediatorResult(chatMessage: ConnectycubeMessage) {
        appExecutors.diskIO().execute {
            chatMessage.attachments?.let {
                attachmentDao.insert(convertToListAttachment(chatMessage)!!)
            }
            messageDao.insert(convertToMessage(chatMessage))
        }
    }

    private fun sendMessage(chatMessage: ConnectycubeMessage,
                            dialog: ConnectycubeDialog
    ): LiveData<Resource<ConnectycubeMessage>> {
        val result = MediatorLiveData<Resource<ConnectycubeMessage>>()
        appExecutors.networkIO().execute {
            try {
                ConnectyCube.chat.sendMessage(chatMessage)
                result.postValue(Resource.success(chatMessage))
            } catch (e: Exception) {
                deleteTempMessage(chatMessage.messageId!!)
                result.postValue(
                    Resource.error(
                        e.message ?: "SmackException.NotConnectedException",
                        chatMessage
                    )
                )
                Timber.d(e)
            } catch (e: InterruptedException) {
                deleteTempMessage(chatMessage.messageId!!)
                result.postValue(Resource.error(e.message ?: "InterruptedException", chatMessage))
                Timber.d(e)
            }
        }
        return result
    }

    private fun createAttachment(path: String, type: String): ConnectycubeAttachment {
        val size = getImageSize(path)

        val attachment = ConnectycubeAttachment(type)
        attachment.id = path.hashCode().toString()
        attachment.url = path
        attachment.height = size.height
        attachment.width = size.width

        return attachment
    }

    private fun createTextMessage(dialog: ConnectycubeDialog): ConnectycubeMessage {
        val chatMessage = ConnectycubeMessage()
        chatMessage.dateSent = System.currentTimeMillis() / 1000
        chatMessage.dialogId = dialog.dialogId
        return chatMessage
    }

    private fun createMessage(dialog: ConnectycubeDialog,
                              text: String
    ): ConnectycubeMessage {
        val chatMessage = ConnectycubeMessage()
        chatMessage.dateSent = System.currentTimeMillis() / 1000
        chatMessage.dialogId = dialog.dialogId
        chatMessage.body = text
        return chatMessage
    }

    private fun updateAttachmentSize(attachmentToTempSave: ConnectycubeAttachment,
                                     attachmentResult: ConnectycubeAttachment
    ) {
        attachmentResult.height = attachmentToTempSave.height
        attachmentResult.width = attachmentToTempSave.width
    }

    private fun buildMessage(messageToTempSave: ConnectycubeMessage,
                             attachment: ConnectycubeAttachment? = null,
                             dialog: ConnectycubeDialog
    ): ConnectycubeMessage {
        val chatMessage = ConnectycubeMessage()
        chatMessage.messageId = messageToTempSave.messageId
        chatMessage.dialogId = messageToTempSave.dialogId
        chatMessage.saveToHistory = true
        chatMessage.dateSent = System.currentTimeMillis() / 1000
        chatMessage.markable = true
        if(dialog.type == ConnectycubeDialogType.PRIVATE) chatMessage.recipientId = dialog.getRecipientId()
        else chatMessage.type = when (dialog.type) {
            ConnectycubeDialogType.GROUP, ConnectycubeDialogType.PUBLIC -> ConnectycubeMessageType.Groupchat
            else -> ConnectycubeMessageType.Chat
        }
        if (attachment != null) {
            chatMessage.attachments!!.add(attachment)
        }
        chatMessage.body = messageToTempSave.body
        return chatMessage
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: MessageSenderRepository? = null

        fun getInstance(messageDao: MessageDao, attachmentDao: AttachmentDao) =
            instance ?: synchronized(this) {
                instance ?: MessageSenderRepository(
                    messageDao,
                    attachmentDao,
                    AppExecutors()
                ).also {
                    instance = it
                }
            }
    }
}