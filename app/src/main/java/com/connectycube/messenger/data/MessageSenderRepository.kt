package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.api.*
import com.connectycube.messenger.utilities.convertToMessage
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.Resource
import org.jivesoftware.smack.SmackException
import timber.log.Timber

class MessageSenderRepository private constructor(private val messageDao: MessageDao,
                                                  private val appExecutors: AppExecutors
) {
    private val service: ConnectycubeService = ConnectycubeService()

    fun sendMessageAttachment(path: String,
                              type: String,
                              text: String,
                              dialog: ConnectycubeChatDialog
    ): LiveData<Resource<ConnectycubeChatMessage>> {
        val messageToTempSave = createMessage(dialog, text)
        messageToTempSave.addAttachment(createAttachment(path, type))
        saveMediatorResult(messageToTempSave)

        val result = MediatorLiveData<Resource<ConnectycubeChatMessage>>()
        result.value = Resource.loading(null)

        val apiResponse = service.loadFileAsAttachment(path, type)
        result.addSource(apiResponse) { response ->
            when (response) {
                is ApiEmptyResponse -> {
                    result.value = Resource.success(null)
                }
                is ApiProgressResponse -> {
                    result.value = Resource.loadingProgress(null, response.progress)
                }
                is ApiErrorResponse -> {
                    result.value = Resource.error(response.errorMessage, null)
                    result.removeSource(apiResponse)
                }
                is ApiSuccessResponse -> {
                    val attachment = response.body

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

    fun sendMessageText(text: String,
                        dialog: ConnectycubeChatDialog
    ): LiveData<Resource<ConnectycubeChatMessage>> {
        val messageToTempSave = createMessage(dialog, text)
        saveMediatorResult(messageToTempSave)

        val result = MediatorLiveData<Resource<ConnectycubeChatMessage>>()
        val messageUpdated = buildMessage(messageToTempSave, dialog = dialog)
        val apiSenderResponse = sendMessage(messageUpdated, dialog)
        result.addSource(apiSenderResponse) {
            result.removeSource(apiSenderResponse)
            result.value = it
        }
        return result
    }

    private fun saveMediatorResult(chatMessage: ConnectycubeChatMessage) {
        appExecutors.diskIO().execute { messageDao.insert(convertToMessage(chatMessage)) }
    }

    private fun sendMessage(chatMessage: ConnectycubeChatMessage,
                            dialog: ConnectycubeChatDialog
    ): LiveData<Resource<ConnectycubeChatMessage>> {
        val result = MediatorLiveData<Resource<ConnectycubeChatMessage>>()
        appExecutors.networkIO().execute {
            try {
                dialog.sendMessage(chatMessage)
                result.postValue(Resource.success(chatMessage))
            } catch (e: SmackException.NotConnectedException) {
                result.postValue(
                    Resource.error(
                        e.message ?: "SmackException.NotConnectedException",
                        chatMessage
                    )
                )
                Timber.d(e)
            } catch (e: InterruptedException) {
                result.postValue(Resource.error(e.message ?: "InterruptedException", chatMessage))
                Timber.d(e)
            }
        }
        return result
    }

    private fun createAttachment(path: String, type: String): ConnectycubeAttachment {
        val attachment = ConnectycubeAttachment(type)
        attachment.url = path
        return attachment
    }

    private fun createTextMessage(dialog: ConnectycubeChatDialog): ConnectycubeChatMessage {
        val chatMessage = ConnectycubeChatMessage()
        chatMessage.dateSent = System.currentTimeMillis() / 1000
        chatMessage.dialogId = dialog.dialogId
        return chatMessage
    }

    private fun createMessage(dialog: ConnectycubeChatDialog,
                              text: String
    ): ConnectycubeChatMessage {
        val chatMessage = ConnectycubeChatMessage()
        chatMessage.dateSent = System.currentTimeMillis() / 1000
        chatMessage.dialogId = dialog.dialogId
        chatMessage.body = text
        return chatMessage
    }

    private fun buildMessage(messageToTempSave: ConnectycubeChatMessage,
                             attachment: ConnectycubeAttachment? = null,
                             dialog: ConnectycubeChatDialog
    ): ConnectycubeChatMessage {
        val chatMessage = ConnectycubeChatMessage()
        chatMessage.id = messageToTempSave.id
        chatMessage.setSaveToHistory(true)
        chatMessage.dateSent = System.currentTimeMillis() / 1000
        chatMessage.isMarkable = true
        if (dialog.isPrivate) chatMessage.recipientId = dialog.recipientId
        if (attachment != null) {
            chatMessage.addAttachment(attachment)
            chatMessage.body = messageToTempSave.body
        } else {
            chatMessage.body = messageToTempSave.body
        }
        return chatMessage
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: MessageSenderRepository? = null

        fun getInstance(messageDao: MessageDao) =
            instance ?: synchronized(this) {
                instance ?: MessageSenderRepository(messageDao, AppExecutors()).also {
                    instance = it
                }
            }
    }
}