package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import com.connectycube.messenger.api.ConnectycubeService
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.NetworkBoundResource
import com.connectycube.messenger.vo.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository private constructor(private val chatDao: ChatDao, private val appExecutors: AppExecutors) {
    val service: ConnectycubeService = ConnectycubeService()

    suspend fun saveChat(chat: Chat) {
        withContext(Dispatchers.IO) {
            chatDao.insert(chat)
        }
    }

    fun getChat(chatId: Int) = chatDao.getChat(chatId)

    fun loadChats(): LiveData<Resource<List<Chat>>> {
        return object : NetworkBoundResource<List<Chat>, List<Chat>>(appExecutors) {
            override fun saveCallResult(item: List<Chat>) {

                chatDao.insertAll(item)
            }

            override fun shouldFetch(data: List<Chat>?) = data.isNullOrEmpty()

            override fun loadFromDb() = chatDao.getChats()

            override fun createCall() = service.loadChats()
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