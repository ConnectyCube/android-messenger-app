package com.connectycube.messenger.utilities

import android.app.Application
import android.content.Context
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.data.*
import com.connectycube.messenger.viewmodels.*

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

    fun provideChatDialogListViewModelFactory(context: Context): ChatDialogListViewModelFactory {
        val repository = getChatRepository(context)
        return ChatDialogListViewModelFactory(repository)
    }

    private fun getChatRepository(context: Context): ChatRepository {
        return ChatRepository.getInstance(
            AppDatabase.getInstance(context.applicationContext).chatDao()
        )
    }

    fun provideChatMessageListViewModelFactory(
        context: Context,
        chat: ConnectycubeChatDialog
    ): ChatMessageListViewModelFactory {
        val repository = getChatMessageRepository(context)
        return ChatMessageListViewModelFactory(repository, chat)
    }

    private fun getChatMessageRepository(context: Context): ChatMessageRepository {
        return ChatMessageRepository.getInstance()
    }

    fun <T, R> provideConnectycubeServiceForType(): LiveDataResponsePerformer<T, R> {
        return LiveDataResponsePerformer()
    }

    fun <T, R> provideConnectycubeServiceProgressForType(): LiveDataResponsePerformerProgress<T, R> {
        return LiveDataResponsePerformerProgress()
    }

    fun provideCreateChatDialogViewModelFactory(application: Application): CreateChatDialogViewModelFactory {
        val usersRepository = getUserRepository(application.baseContext)
        val chatRepository = getChatRepository(application.baseContext)
        return CreateChatDialogViewModelFactory(application, usersRepository, chatRepository)
    }

    private fun getAttachmentViewRepository(): AttachmentRepository {
        return AttachmentRepository.getInstance()
    }

    fun provideAttachmentViewModelFactory(application: Application): AttachmentViewModelFactory {
        val repository = getAttachmentViewRepository()
        return AttachmentViewModelFactory(application, repository)
    }
}