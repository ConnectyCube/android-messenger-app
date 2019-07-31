package com.connectycube.messenger.utilities

import android.content.Context
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.data.AppDatabase
import com.connectycube.messenger.data.ChatMessageRepository
import com.connectycube.messenger.data.ChatRepository
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.viewmodels.ChatListViewModelFactory
import com.connectycube.messenger.viewmodels.MessageListViewModelFactory
import com.connectycube.messenger.viewmodels.UserListViewModelFactory

object InjectorUtils {

    fun provideUserListViewModelFactory(context: Context, logins: ArrayList<String>): UserListViewModelFactory {
        val repository = getUserRepository(context)
        return UserListViewModelFactory(repository, logins)
    }

    private fun getUserRepository(context: Context): UserRepository {
        return UserRepository.getInstance(
            AppDatabase.getInstance(context.applicationContext).userDao()
        )
    }

    fun provideChatListViewModelFactory(context: Context): ChatListViewModelFactory {
        val repository = getChatRepository(context)
        return ChatListViewModelFactory(repository)
    }

    private fun getChatRepository(context: Context): ChatRepository {
        return ChatRepository.getInstance(
            AppDatabase.getInstance(context.applicationContext).chatDao()
        )
    }

    fun provideMessageListViewModelFactory(context: Context, chat: ConnectycubeChatDialog): MessageListViewModelFactory {
        val repository = getChatMessageRepository(context)
        return MessageListViewModelFactory(repository, chat)
    }

    private fun getChatMessageRepository(context: Context): ChatMessageRepository {
        return ChatMessageRepository.getInstance()
    }

    fun <T, R> provideConnectycubeServiceForType(): LiveDataResponsePerformer<T, R> {
        return LiveDataResponsePerformer()
    }
}