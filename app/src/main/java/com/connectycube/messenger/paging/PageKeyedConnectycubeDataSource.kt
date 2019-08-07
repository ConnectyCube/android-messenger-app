package com.connectycube.messenger.paging

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.connectycube.chat.ConnectycubeRestChatService
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.request.MessageGetBuilder
import com.connectycube.core.exception.ResponseException
import java.util.concurrent.Executor

const val SKIP = 20

class PageKeyedConnectycubeDataSource(
    private val chatDialog: ConnectycubeChatDialog,
    private val retryExecutor: Executor
) : PageKeyedDataSource<Int, ConnectycubeChatMessage>() {

    // keep a function reference for the retry event
    private var retry: (() -> Any)? = null

    /**
     * There is no sync on the state because paging will always call loadInitial first then wait
     * for it to return some success value before calling loadAfter.
     */
    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retryAllFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            retryExecutor.execute {
                it.invoke()
            }
        }
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, ConnectycubeChatMessage>
    ) {
        // ignored, since we only ever append to our initial load
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, ConnectycubeChatMessage>) {
        networkState.postValue(NetworkState.LOADING)
        val messageGetBuilder = MessageGetBuilder()

        messageGetBuilder.skip = params.key
        messageGetBuilder.limit = params.requestedLoadSize
        messageGetBuilder.sortDesc("date_sent")
        messageGetBuilder.markAsRead(false)

        val performer = ConnectycubeRestChatService.getDialogMessages(chatDialog, messageGetBuilder)
        try {
            val messages = performer.perform()
            val items = messages.map { it.body } ?: emptyList()
            retry = null
            callback.onResult(messages, SKIP)
            networkState.postValue(NetworkState.LOADED)
        } catch (ex: ResponseException) {
            retry = {
                loadAfter(params, callback)
            }
            networkState.postValue(NetworkState.error(ex.message ?: "unknown err"))
        }
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, ConnectycubeChatMessage>
    ) {
        val messageGetBuilder = MessageGetBuilder()

        messageGetBuilder.skip = 0
        messageGetBuilder.limit = params.requestedLoadSize
        messageGetBuilder.sortDesc("date_sent")
        messageGetBuilder.markAsRead(false)

        val performer = ConnectycubeRestChatService.getDialogMessages(chatDialog, messageGetBuilder)

        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)

        // triggered by a refresh, we better execute sync
        try {

            val response = performer.perform()
            retry = null
            networkState.postValue(NetworkState.LOADED)
            initialLoad.postValue(NetworkState.LOADED)
            callback.onResult(response, 0, SKIP)
        } catch (ex: ResponseException) {
            retry = {
                loadInitial(params, callback)
            }
            val error = NetworkState.error(ex.message ?: "unknown error")
            networkState.postValue(error)
            initialLoad.postValue(error)
        }
    }
}