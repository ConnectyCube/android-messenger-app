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

    fun provideUserViewModelFactory(application: Application, userId: Int): UserDetailsViewModelFactory {
        val repository = getUserRepository(application.baseContext)
        return UserDetailsViewModelFactory(application, userId, repository)
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
        application: Application,
        chat: ConnectycubeChatDialog
    ): ChatMessageListViewModelFactory {
        val chatRepository = getChatMessageRepository(application.baseContext)
        val userRepository = getUserRepository(application.baseContext)
        return ChatMessageListViewModelFactory(application, chatRepository, userRepository, chat)
    }

    private fun getChatMessageRepository(context: Context): ChatMessageRepository {
        return ChatMessageRepository.getInstance(context.applicationContext)
    }

    fun <T, R> provideConnectycubeServiceForType(): LiveDataResponsePerformer<T, R> {
        return LiveDataResponsePerformer()
    }

    fun <T, R> provideSyncConnectycubeServiceForType(): ResponsePerformer<T, R> {
        return ResponsePerformer()
    }

    fun <T, R> provideConnectycubeServiceProgressForType(): LiveDataResponsePerformerProgress<T, R> {
        return LiveDataResponsePerformerProgress()
    }

    fun provideCreateChatDialogViewModelFactory(application: Application): CreateChatDialogViewModelFactory {
        val usersRepository = getUserRepository(application.baseContext)
        val chatRepository = getChatRepository(application.baseContext)
        return CreateChatDialogViewModelFactory(application, usersRepository, chatRepository)
    }

    fun provideChatDialogDetailsViewModelFactory(
        application: Application,
        dialogId: String
    ): ChatDialogDetailsViewModelFactory {
        val usersRepository = getUserRepository(application.baseContext)
        val chatRepository = getChatRepository(application.baseContext)
        return ChatDialogDetailsViewModelFactory(application, dialogId, usersRepository, chatRepository)
    }

    private fun getAttachmentViewRepository(): AttachmentRepository {
        return AttachmentRepository.getInstance()
    }

    fun provideAttachmentViewModelFactory(application: Application): AttachmentViewModelFactory {
        val repository = getAttachmentViewRepository()
        return AttachmentViewModelFactory(application, repository)
    }

    private fun getAvatarRepositoryViewRepository(): AvatarRepository {
        return AvatarRepository.getInstance()
    }

    fun provideCreateDialogDetailsViewModelFactory(application: Application): CreateDialogDetailsViewModelFactory {
        val repository = getAvatarRepositoryViewRepository()
        return CreateDialogDetailsViewModelFactory(application, repository)
    }
}