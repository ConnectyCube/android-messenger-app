package com.connectycube.messenger.api

import androidx.lifecycle.LiveData
import com.connectycube.chat.ConnectycubeRestChatService
import com.connectycube.chat.Consts
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.request.MessageGetBuilder
import com.connectycube.core.request.RequestGetBuilder
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.Message
import com.connectycube.messenger.data.User
import com.connectycube.messenger.utilities.*
import com.connectycube.storage.ConnectycubeStorage
import com.connectycube.storage.model.ConnectycubeFile
import com.connectycube.users.ConnectycubeUsers
import com.connectycube.users.model.ConnectycubeUser
import timber.log.Timber
import java.io.File

class ConnectycubeService {

    fun loadUsers(usersLogins: ArrayList<String>): LiveData<ApiResponse<List<User>>> {

        return InjectorUtils.provideConnectycubeServiceForType<ArrayList<ConnectycubeUser>, List<User>>()
            .perform(
                ConnectycubeUsers.getUsersByLogins(usersLogins, null),
                object : Converter<List<User>, ArrayList<ConnectycubeUser>>() {
                    override fun convertTo(response: ArrayList<ConnectycubeUser>): List<User> {
                        return convertToUsers(response)
                    }
                })
    }

    fun loadChatsSlice(): LiveData<ApiResponse<List<Chat>>> {
        val requestGetBuilder = RequestGetBuilder().apply { limit = 10 }
        return InjectorUtils.provideConnectycubeServiceForType<ArrayList<ConnectycubeChatDialog>, List<Chat>>()
            .perform(
                ConnectycubeRestChatService.getChatDialogs(null, requestGetBuilder),
                object : Converter<List<Chat>, ArrayList<ConnectycubeChatDialog>>() {
                    override fun convertTo(response: ArrayList<ConnectycubeChatDialog>): List<Chat> {
                        return convertToChats(response)
                    }
                })
    }

    fun loadChats(): LiveData<ApiResponse<List<Chat>>> {
        val requestBuilder = RequestGetBuilder()
            .sortDesc(Consts.DIALOG_LAST_MESSAGE_DATE_SENT_FIELD_NAME)
            .`in`(Consts.DIALOG_TYPE, 2, 3)

        return InjectorUtils.provideConnectycubeServiceForType<ArrayList<ConnectycubeChatDialog>, List<Chat>>()
            .perform(
                ConnectycubeRestChatService.getChatDialogs(null, requestBuilder),
                object : Converter<List<Chat>, ArrayList<ConnectycubeChatDialog>>() {
                    override fun convertTo(response: ArrayList<ConnectycubeChatDialog>): List<Chat> {
                        return convertToChats(response)
                    }
                })
    }

    fun createChatDialog(chat: Chat): LiveData<ApiResponse<Chat>> {
        val chatDialog: ConnectycubeChatDialog = chat.conChat

        return InjectorUtils.provideConnectycubeServiceForType<ConnectycubeChatDialog, Chat>()
            .perform(
                ConnectycubeRestChatService.createChatDialog(chatDialog),
                object : Converter<Chat, ConnectycubeChatDialog>() {
                    override fun convertTo(response: ConnectycubeChatDialog): Chat {
                        return Chat(
                            response.dialogId,
                            response.lastMessageDateSent,
                            response.createdAt.time,
                            response.unreadMessageCount ?: 0,
                            response.name,
                            response
                        )
                    }
                })
    }

    fun loadFileAsAttachment(path: String, type: String): LiveData<ApiResponse<ConnectycubeAttachment>> {
        val file = File(path)
        Timber.d("loadFileAsAttachment path= $path")
        val service =
            InjectorUtils.provideConnectycubeServiceProgressForType<ConnectycubeFile, ConnectycubeAttachment>()
        return service.perform(
            ConnectycubeStorage.uploadFileTask(
                file, true
            ) { service.progressCallBack.onProgressUpdate(it) },
            object : Converter<ConnectycubeAttachment, ConnectycubeFile>() {
                override fun convertTo(response: ConnectycubeFile): ConnectycubeAttachment {
                    val attachment = ConnectycubeAttachment(type)
                    attachment.id = response.id.toString()
                    attachment.url = response.publicUrl
                    return attachment
                }
            })
    }

    fun loadMessages(dialogId: String, messageGetBuilder: MessageGetBuilder): LiveData<ApiResponse<List<Message>>> {
        return InjectorUtils.provideConnectycubeServiceForType<ArrayList<ConnectycubeChatMessage>, List<Message>>()
            .perform(
                ConnectycubeRestChatService.getDialogMessages(ConnectycubeChatDialog(dialogId), messageGetBuilder),
                object : Converter<List<Message>, ArrayList<ConnectycubeChatMessage>>() {
                    override fun convertTo(response: ArrayList<ConnectycubeChatMessage>): List<Message> {
                        return convertToMessages(response)
                    }
                })
    }
}