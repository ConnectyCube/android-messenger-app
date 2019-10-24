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
        val chatRepository = getChatRepository(context)
        val messageRepository = getChatMessageRepository(context)
        return ChatDialogListViewModelFactory(chatRepository, messageRepository)
    }

    private fun getChatRepository(context: Context): ChatRepository {
        return ChatRepository.getInstance(
            AppDatabase.getInstance(context.applicationContext).chatDao()
        )
    }

    fun provideChatMessageListViewModelFactory(
        application: Application,
        dialogId: String
    ): ChatMessageListViewModelFactory {
        val chatMessageRepository = getChatMessageRepository(application.baseContext)
        val userRepository = getUserRepository(application.baseContext)
        val chatRepository = getChatRepository(application.baseContext)
        return ChatMessageListViewModelFactory(application, chatMessageRepository, userRepository, chatRepository, dialogId)
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

    fun provideSelectUsersViewModelFactory(application: Application): SelectUsersViewModelFactory {
        val usersRepository = getUserRepository(application.baseContext)
        return SelectUsersViewModelFactory(application, usersRepository)
    }

    fun provideSelectFromExistUsersViewModelFactory(application: Application): SelectFromExistUsersViewModelFactory {
        val usersRepository = getUserRepository(application.baseContext)
        return SelectFromExistUsersViewModelFactory(application, usersRepository)
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

    fun provideCallViewModelFactory(application: Application): CallViewModelFactory {
        val userRepository = getUserRepository(application)
        return CallViewModelFactory(application, userRepository)
    }

    private fun getMessageSenderViewRepository(context: Context): MessageSenderRepository {
        return MessageSenderRepository.getInstance(
            AppDatabase.getInstance(context.applicationContext).messageDao(),
            AppDatabase.getInstance(context.applicationContext).attachmentDao()
        )
    }

    fun provideMessageSenderViewModelFactory(application: Application,
                                             dialog: ConnectycubeChatDialog
    ):  MessageSenderViewModelFactory {
        val repository = getMessageSenderViewRepository(application)
        return MessageSenderViewModelFactory(application, dialog, repository)
    }
}