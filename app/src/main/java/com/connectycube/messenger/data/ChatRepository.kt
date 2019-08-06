package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import com.connectycube.messenger.api.ApiResponse
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.connectycube.messenger.api.ConnectycubeService
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.NetworkBoundResource
import com.connectycube.messenger.vo.Resource
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
                chatDao.insert(item)
            }

            override fun shouldFetch(data: Chat?, newData: Chat?) = data == null

            override fun loadFromDb() = chatDao.getChat(newChatDialogId)

            override fun createCall() = service.createChatDialog(chat)
        }.asLiveData()
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