package com.connectycube.messenger.data

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations
import com.connectycube.chat.model.ConnectycubeChatDialog
import androidx.paging.toLiveData
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.paging.Listing
import com.connectycube.messenger.paging.SubConnectycubeDataSourceFactory
import com.connectycube.messenger.vo.AppExecutors

class ChatMessageRepository(private val appExecutors: AppExecutors) {

    @MainThread
    fun loadDialogMessages(dialog: ConnectycubeChatDialog, pageSize: Int): Listing<ConnectycubeChatMessage> {
        val sourceFactory =
            SubConnectycubeDataSourceFactory(dialog, appExecutors.networkIO())

        // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
        val livePagedList = sourceFactory.toLiveData(
            pageSize = pageSize,
            // provide custom executor for network requests, otherwise it will default to
            // Arch Components' IO pool which is also used for disk access
            fetchExecutor = appExecutors.networkIO()
        )

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        return Listing(
            pagedList = livePagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveData.value?.retryAllFailed()
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: ChatMessageRepository? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ChatMessageRepository(AppExecutors()).also { instance = it }
            }
    }
}