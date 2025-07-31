package com.connectycube.messenger.api

import androidx.lifecycle.LiveData
import com.connectycube.ConnectyCube
import com.connectycube.chat.models.ConnectycubeAttachment
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeDialogType
import com.connectycube.chat.models.ConnectycubeMessage
import com.connectycube.core.utils.FILTER_LOGIN
import com.connectycube.core.utils.QueryRule
import com.connectycube.messenger.data.Chat
import com.connectycube.messenger.data.Message
import com.connectycube.messenger.data.User
import com.connectycube.messenger.utilities.Converter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.ResponsePerformer
import com.connectycube.messenger.utilities.convertToChat
import com.connectycube.messenger.utilities.convertToChats
import com.connectycube.messenger.utilities.convertToMessages
import com.connectycube.messenger.utilities.convertToUser
import com.connectycube.messenger.utilities.convertToUsers
import com.connectycube.pushnotifications.models.ConnectycubePushEvent
import com.connectycube.pushnotifications.queries.CreateEventQuery
import com.connectycube.storage.models.ConnectycubeFile
import com.connectycube.users.models.ConnectycubeUser
import timber.log.Timber
import java.io.File

class ConnectycubeService {

    fun loadUsers(usersLogins: List<String>): LiveData<ApiResponse<List<User>>> {
        val params: Map<String, Any> = hashMapOf(FILTER_LOGIN + "[${QueryRule.IN}]" to usersLogins)

        return InjectorUtils.provideConnectycubeServiceForType<List<ConnectycubeUser>, List<User>>()
            .perform({ ConnectyCube.getUsers(params).items },
                     object : Converter<List<User>, List<ConnectycubeUser>>() {
                         override fun convertTo(response: List<ConnectycubeUser>): List<User> {
                             return convertToUsers(response)
                         }
                     })
    }

    fun updateUserName(userId: Int, newName: String): LiveData<ApiResponse<User>> {
        val user = ConnectycubeUser(fullName = newName).apply{ id = userId}
        return InjectorUtils.provideConnectycubeServiceForType<ConnectycubeUser, User>()
            .perform({ ConnectyCube.updateUser(user) },
                     object : Converter<User, ConnectycubeUser>() {
                         override fun convertTo(response: ConnectycubeUser): User {
                             return convertToUser(response)
                         }
                     })
    }

    fun updateUserAvatar(userId: Int, newAvatarPath: String): LiveData<ApiResponse<User>> {
        val service =
            InjectorUtils.provideConnectycubeServiceProgressForType<ConnectycubeUser, User>()
        return service.perform({ uploadUserAvatarTask(userId, newAvatarPath) },
                               object : Converter<User, ConnectycubeUser>() {
                                   override fun convertTo(response: ConnectycubeUser): User {
                                       return convertToUser(response)
                                   }
                               })
    }

    private suspend fun uploadUserAvatarTask(userId: Int, path: String): ConnectycubeUser {
        try {
            val file: ConnectycubeFile = ConnectyCube.uploadFile(path, true)
            return ConnectyCube.updateUser(ConnectycubeUser().apply { id = userId; avatar = file.getPublicUrl()})
        } catch (ex: Exception) {
            throw ex
        }
    }

    fun updateChatSync(dialogId: String, callback: ResponsePerformer.Callback<Chat>) {
        val params: HashMap<String, Any> = hashMapOf("_id" to dialogId)
        InjectorUtils.provideSyncConnectycubeServiceForType<ConnectycubeDialog, Chat>()
            .perform(
                { ConnectyCube.getDialogs(params).items.first() },
                object : Converter<Chat, ConnectycubeDialog>() {
                    override fun convertTo(response: ConnectycubeDialog): Chat {
                        return convertToChat(response)
                    }
                }, callback
            )
    }

    fun loadChatsSlice(): LiveData<ApiResponse<List<Chat>>> {
        val params: HashMap<String, Any> = hashMapOf(
            "limit" to 10
        )
        return InjectorUtils.provideConnectycubeServiceForType<List<ConnectycubeDialog>, List<Chat>>()
            .perform({ ConnectyCube.getDialogs(params).items },
                     object : Converter<List<Chat>, List<ConnectycubeDialog>>() {
                         override fun convertTo(response: List<ConnectycubeDialog>): List<Chat> {
                             return convertToChats(response)
                         }
                     })
    }

    fun loadChats(): LiveData<ApiResponse<List<Chat>>> {
        val params:Map<String, Any>  = hashMapOf(
            "sort_desc" to "last_message_date_sent",
            "type[in]" to "${ConnectycubeDialogType.GROUP},${ConnectycubeDialogType.PRIVATE}"
        )

        return InjectorUtils.provideConnectycubeServiceForType<List<ConnectycubeDialog>, List<Chat>>()
            .perform({ ConnectyCube.getDialogs(params).items },
                     object : Converter<List<Chat>, List<ConnectycubeDialog>>() {
                         override fun convertTo(response: List<ConnectycubeDialog>): List<Chat> {
                             return convertToChats(response)
                         }
                     })
    }

    fun createChatDialog(chat: Chat): LiveData<ApiResponse<Chat>> {
        return InjectorUtils.provideConnectycubeServiceForType<ConnectycubeDialog, Chat>()
            .perform({ ConnectyCube.createDialog(chat) },
                     object : Converter<Chat, ConnectycubeDialog>() {
                         override fun convertTo(response: ConnectycubeDialog): Chat {
                             return convertToChat(response)
                         }
                     })
    }

    fun loadFileAsAttachment(path: String, type: String): LiveData<ApiResponse<ConnectycubeAttachment>> {
        val file = File(path)

        Timber.d("loadFileAsAttachment path= $path")
        val service =
            InjectorUtils.provideConnectycubeServiceProgressForType<ConnectycubeFile, ConnectycubeAttachment>()
        return service.perform({ ConnectyCube.uploadFile(file.path, true) },
                               object : Converter<ConnectycubeAttachment, ConnectycubeFile>() {
                                   override fun convertTo(response: ConnectycubeFile): ConnectycubeAttachment {
                                       val attachment = ConnectycubeAttachment(type)
                                       attachment.id = response.id.toString()
                                       attachment.url = response.getPublicUrl()
                                       return attachment
                                   }
                               })
    }

    fun loadMessages(dialogId: String, params: Map<String, Any>): LiveData<ApiResponse<List<Message>>> {
        return InjectorUtils.provideConnectycubeServiceForType<List<ConnectycubeMessage>, List<Message>>()
            .perform({ ConnectyCube.getMessages(dialogId, params).items },
                     object : Converter<List<Message>, List<ConnectycubeMessage>>() {
                         override fun convertTo(response: List<ConnectycubeMessage>): List<Message> {
                             return convertToMessages(response)
                         }
                     })
    }

    fun deleteDialog(vararg dialogId: String, forceDelete: Boolean): LiveData<ApiResponse<List<String>>>{
        return InjectorUtils.provideConnectycubeServiceForType<List<String>, Void>()
            .perform { ConnectyCube.deleteDialogs(dialogId.toSet(), forceDelete).successfullyDeleted!!.ids}
    }

    fun updateDialogDescription(dialogId: String, newGroupDescription: String): LiveData<ApiResponse<Chat>> {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)

        return updateChatDialog(chatDialog, mapOf("description" to newGroupDescription))
    }

    fun updateDialogDescription(dialogId: String,
                                newGroupDescription: String,
                                callback: ResponsePerformer.Callback<Chat>
    ) {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)

        updateChatDialogSync(chatDialog, mapOf("description" to newGroupDescription), callback)
    }

    fun updateDialogName(dialogId: String, newGroupName: String): LiveData<ApiResponse<Chat>> {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)

        return updateChatDialog(chatDialog, mapOf("name" to newGroupName))
    }

    fun updateDialogName(dialogId: String,
                         newGroupName: String,
                         callback: ResponsePerformer.Callback<Chat>
    ) {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)

        updateChatDialogSync(chatDialog, mapOf("name" to newGroupName), callback)
    }

    fun updateDialogPhoto(dialogId: String, newGroupPhotoUrl: String): LiveData<ApiResponse<Chat>> {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        chatDialog.photo = newGroupPhotoUrl

        return updateChatDialog(chatDialog)
    }

    fun updateDialogPhoto(dialogId: String,
                          newGroupPhotoUrl: String,
                          callback: ResponsePerformer.Callback<Chat>
    ) {
        val file = File(newGroupPhotoUrl)
        val service =
            InjectorUtils.provideSyncConnectycubeServiceForType<ConnectycubeDialog, Chat>()
        service.performProgress(
            { uploadDialogPhotoTask(dialogId, file.path, true) },
            object : Converter<Chat, ConnectycubeDialog>() {
                override fun convertTo(response: ConnectycubeDialog): Chat {
                    return convertToChat(response)
                }
            }, callback
        )
    }

    private suspend fun uploadDialogPhotoTask(dialogId: String, path: String, publicAccess: Boolean): ConnectycubeDialog {
        try {
            val file: ConnectycubeFile = ConnectyCube.uploadFile(path, true)
            val params:Map<String, Any> = hashMapOf(
                "photo" to file.getPublicUrl()!!
            )
            return ConnectyCube.updateDialog(dialogId, params)
        } catch (ex: Exception) {
            throw ex
        }
    }

    fun addDialogOccupants(dialogId: String, vararg usersIds: Int): LiveData<ApiResponse<Chat>> {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        val params: Map<String, Any> = hashMapOf(
            "push_all" to hashMapOf("occupants_ids" to usersIds.toList())
        )
        return updateChatDialog(chatDialog, params)
    }

    fun addDialogOccupants(dialogId: String,
                           vararg usersIds: Int,
                           callback: ResponsePerformer.Callback<Chat>
    ) {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        val params: Map<String, Any> = hashMapOf(
            "push_all" to hashMapOf("occupants_ids" to usersIds.toList())
        )
        updateChatDialogSync(chatDialog, params, callback)
    }

    fun removeDialogOccupants(dialogId: String, vararg usersIds: Int): LiveData<ApiResponse<Chat>> {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        val params: Map<String, Any> = hashMapOf(
            "pull_all" to hashMapOf("occupants_ids" to usersIds.toList())
        )

        return updateChatDialog(chatDialog, params)
    }

    fun removeDialogOccupants(dialogId: String,
                              vararg usersIds: Int,
                              callback: ResponsePerformer.Callback<Chat>
    ) {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        val params: Map<String, Any> = hashMapOf(
            "pull_all" to hashMapOf("occupants_ids" to usersIds.toList())
        )

        updateChatDialogSync(chatDialog, params, callback)
    }

    fun addDialogAdmins(dialogId: String, vararg usersIds: Int): LiveData<ApiResponse<Chat>> {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        val params: Map<String, Any> = hashMapOf(
            "push_all" to hashMapOf("admins_ids" to usersIds.toList())
        )

        return updateChatDialog(chatDialog, params)
    }

    fun addDialogAdmins(dialogId: String,
                        vararg usersIds: Int,
                        callback: ResponsePerformer.Callback<Chat>
    ) {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        val params: Map<String, Any> = hashMapOf(
            "push_all" to hashMapOf("admins_ids" to usersIds.toList())
        )

        updateChatDialogSync(chatDialog, params, callback)
    }

    fun removeDialogAdmins(dialogId: String, vararg usersIds: Int): LiveData<ApiResponse<Chat>> {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        val params: Map<String, Any> = hashMapOf(
            "pull_all" to hashMapOf("admins_ids" to usersIds.toList())
        )

        return updateChatDialog(chatDialog, params)
    }

    fun removeDialogAdmins(dialogId: String,
                           vararg usersIds: Int,
                           callback: ResponsePerformer.Callback<Chat>
    ) {
        val chatDialog = ConnectycubeDialog(dialogId = dialogId)
        val params: Map<String, Any> = hashMapOf(
            "pull_all" to hashMapOf("admins_ids" to usersIds.toList())
        )

        updateChatDialogSync(chatDialog, params, callback)
    }

    private fun updateChatDialog(chatDialog: ConnectycubeDialog): LiveData<ApiResponse<Chat>> {
        return updateChatDialog(chatDialog, null)
    }

    private fun updateChatDialogSync(chatDialog: ConnectycubeDialog,
                                     callback: ResponsePerformer.Callback<Chat>
    ) {
        return updateChatDialogSync(chatDialog, null, callback)
    }

    private fun updateChatDialog(
        chatDialog: ConnectycubeDialog,
        params: Map<String, Any>?
    ): LiveData<ApiResponse<Chat>> {
        return InjectorUtils.provideConnectycubeServiceForType<ConnectycubeDialog, Chat>()
            .perform({ ConnectyCube.updateDialog(chatDialog.dialogId!!, params)},
                object : Converter<Chat, ConnectycubeDialog>() {
                    override fun convertTo(response: ConnectycubeDialog): Chat {
                        return convertToChat(response)
                    }
                })
    }

    private fun updateChatDialogSync(
        chatDialog: ConnectycubeDialog,
        params: Map<String, Any>?,
        callback: ResponsePerformer.Callback<Chat>
    ) {
        InjectorUtils.provideSyncConnectycubeServiceForType<ConnectycubeDialog, Chat>()
            .perform({ ConnectyCube.updateDialog(chatDialog.dialogId!!, params)},
                object : Converter<Chat, ConnectycubeDialog>() {
                    override fun convertTo(response: ConnectycubeDialog): Chat {
                        return convertToChat(response)
                    }
                }, callback
            )
    }

    fun uploadAvatar(avatarPath: String): LiveData<ApiResponse<String>> {
        val file = File(avatarPath)
        val service =
            InjectorUtils.provideConnectycubeServiceProgressForType<ConnectycubeFile, String>()
        return service.perform({ ConnectyCube.uploadFile(file.path, true) },
            object : Converter<String, ConnectycubeFile>() {
                override fun convertTo(response: ConnectycubeFile): String {
                    return response.getPublicUrl()?:""
                }
            })
    }

    fun createPushEvent(event: ConnectycubePushEvent,
                        callback: ResponsePerformer.Callback<ConnectycubePushEvent>?
    ) {
        InjectorUtils.provideSyncConnectycubeServiceForType<ConnectycubePushEvent, ConnectycubePushEvent>()
            .perform({ ConnectyCube.createPushEvent(event).first()}, callback)
    }
}