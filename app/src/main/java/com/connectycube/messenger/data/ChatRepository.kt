package com.connectycube.messenger.data

import android.os.Bundle
import androidx.lifecycle.LiveData
import com.connectycube.core.Consts
import com.connectycube.messenger.api.ApiResponse
import com.connectycube.messenger.api.ConnectycubeService
import com.connectycube.messenger.utilities.UpdateResourceProcessor
import com.connectycube.messenger.vo.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ChatRepository private constructor(private val chatDao: ChatDao, private val appExecutors: AppExecutors) {
    val service: ConnectycubeService = ConnectycubeService()

    suspend fun saveChat(chat: Chat) {
        withContext(Dispatchers.IO) {
            chatDao.insert(chat)
        }
    }

    fun getChat(chatId: String) = chatDao.getChat(chatId)

    fun getChatSync(chatId: String) = chatDao.getChatSync(chatId)

    fun update(dialogId: String): LiveData<Resource<List<Chat>>> {
        return loadChats()
    }

    fun loadChats(): LiveData<Resource<List<Chat>>> {
        return object : NetworkBoundResource<List<Chat>, List<Chat>>(appExecutors) {

            override fun createCallSlice() = service.loadChatsSlice()

            override fun saveCallResult(item: List<Chat>) {

                chatDao.insertAll(item)
            }

            override fun shouldFetch(data: List<Chat>?, newData: List<Chat>?): Boolean {
                val oldData = data?.take(10)
                val result = data != null && oldData != newData
                Timber.d("shouldFetch result= $result")
                return result
            }

            override fun loadFromDb() = chatDao.getChats()

            override fun createCall() = service.loadChats()
            override fun shouldShowMediateResult(data: List<Chat>?) = !data.isNullOrEmpty()
        }.asLiveData()
    }

    fun createChatDialog(chat: Chat): LiveData<Resource<Chat>> {
        return object : NetworkBoundResource<Chat, Chat>(appExecutors) {
            var newChatDialogId: String? = null

            override fun saveCallResult(item: Chat) {
                newChatDialogId = item.chatId
                //TODO VT to save correct name for private dialog (delete TODO code after server fix and uncomment stable code)
                item.cubeChat.name = chat.name
                val createdDialog = item.copy(name = chat.name)
                chatDao.insert(createdDialog)
                //TODO end

//                chatDao.insert(item)
            }

            override fun shouldFetch(data: Chat?, newData: Chat?) = data == null

            override fun loadFromDb() = chatDao.getChat(newChatDialogId)

            override fun createCall() = service.createChatDialog(chat)
        }.asLiveData()
    }

    fun deleteChat(force: Boolean, chat: Chat): LiveData<Resource<List<Chat>>>{
        return deleteChats(force, chatsIds = *arrayOf(chat.chatId))
    }

    fun deleteChats(force: Boolean, vararg chatsIds: String): LiveData<Resource<List<Chat>>> {
        return object : DeleteNetworkBoundResource<List<Chat>, ArrayList<String>>(appExecutors) {
            override fun saveCallResult(item: ArrayList<String>) {
            }

            override fun createCall() = service.deleteDialog(dialogId = *chatsIds, forceDelete = force)

            override fun processResponseBundle(bundle: Bundle?) {
                Timber.d("bundle= $bundle")
                if (bundle == null) return
//                val notFoundIds: ArrayList<String>  = bundle.getStringArrayList(Consts.NOT_FOUND_IDS)
//                val wrongPermissionsIds: ArrayList<String>  = bundle.getStringArrayList(Consts.WRONG_PERMISSIONS_IDS)

                val successfullyDeletedIds: ArrayList<String>? = bundle.getStringArrayList(Consts.SUCCESSFULLY_DELETED_IDS)
                if (!successfullyDeletedIds.isNullOrEmpty()) {
                    val array = arrayOfNulls<String>(successfullyDeletedIds.size)
                    successfullyDeletedIds.toArray(array)
                    chatDao.deleteChatsByIds(*array)
                }
            }
        }.asLiveData()
    }

    fun updateChatName(chatId: String, newChatName: String): LiveData<Resource<Chat>> {
        return updateChat(chatId) {service.updateDialogName(chatId, newChatName)}
    }

    fun updateChatName(chatId: String, newChatName: String, errorAction: Function2<String, Chat, Unit>) {
        service.updateDialogName(chatId, newChatName, getRequestProcessor(chatId, errorAction))
    }

    fun updateChatDescription(chatId: String, newChatDescription: String): LiveData<Resource<Chat>> {
        return updateChat(chatId) {service.updateDialogDescription(chatId, newChatDescription)}
    }

    fun updateChatDescription(chatId: String, newChatDescription: String, errorAction: Function2<String, Chat, Unit>) {
        service.updateDialogDescription(chatId, newChatDescription, getRequestProcessor(chatId, errorAction))
    }

    fun updateChatPhoto(chatId: String, newChatPhoto: String): LiveData<Resource<Chat>> {
        return updateChat(chatId) {service.updateDialogPhoto(chatId, newChatPhoto)}
    }

    fun addChatOccupants(chatId: String, vararg usersIds: Int): LiveData<Resource<Chat>> {
        return updateChat(chatId) {service.addDialogOccupants(chatId, *usersIds)}
    }

    fun addChatOccupants(chatId: String, vararg usersIds: Int, errorAction: Function2<String, Chat, Unit>) {
        service.addDialogOccupants(chatId, usersIds = *usersIds, callback = getRequestProcessor(chatId, errorAction))
    }

    fun removeChatOccupants(chatId: String, vararg usersIds: Int): LiveData<Resource<Chat>> {
        return updateChat(chatId) {service.removeDialogOccupants(chatId, *usersIds)}
    }

    fun removeChatOccupants(chatId: String, vararg usersIds: Int, errorAction: Function2<String, Chat, Unit>) {
        service.removeDialogOccupants(chatId, usersIds = *usersIds, callback = getRequestProcessor(chatId, errorAction))
    }

    fun addChatAdmins(chatId: String, vararg usersIds: Int): LiveData<Resource<Chat>> {
        return updateChat(chatId) {service.addDialogAdmins(chatId, *usersIds)}
    }

    fun addChatAdmins(chatId: String, vararg usersIds: Int, errorAction: Function2<String, Chat, Unit>) {
        service.addDialogAdmins(chatId, usersIds = *usersIds, callback = getRequestProcessor(chatId, errorAction))
    }

    fun removeChatAdmins(chatId: String, vararg usersIds: Int): LiveData<Resource<Chat>> {
        return updateChat(chatId) {service.removeDialogAdmins(chatId, *usersIds)}
    }

    fun removeChatAdmins(chatId: String, vararg usersIds: Int, errorAction: Function2<String, Chat, Unit>) {
        service.removeDialogAdmins(chatId, usersIds = *usersIds, callback = getRequestProcessor(chatId, errorAction))
    }

    private fun updateChat(chatId: String, function: Function0<LiveData<ApiResponse<Chat>>>): LiveData<Resource<Chat>> {
        return object : UpdateNetworkBoundResource<Chat, Chat>(appExecutors) {
            override fun saveCallResult(item: Chat) {
                chatDao.insert(item)
            }

            override fun loadFromDb(): LiveData<Chat> {
                return chatDao.getChat(chatId)
            }

            override fun createCall() = function.invoke()

        }.asLiveData()
    }

    private fun getRequestProcessor(chatId: String, errorAction: Function2<String, Chat, Unit>): UpdateResourceProcessor<Chat> {
        return object : UpdateResourceProcessor<Chat>(appExecutors){
            override fun processError(errorMessage: String) {
                errorAction.invoke(errorMessage, chatDao.getChatSync(chatId))
            }

            override fun saveCallResult(item: Chat) {
                chatDao.update(item)
            }
        }
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: ChatRepository? = null

        fun getInstance(chatDao: ChatDao) =
            instance ?: synchronized(this) {
                instance ?: ChatRepository(chatDao, AppExecutors()).also { instance = it }
            }
    }
}