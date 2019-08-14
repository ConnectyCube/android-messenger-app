package com.connectycube.messenger.paging

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.api.ChatMessageApi
import com.connectycube.messenger.data.Message
import com.connectycube.messenger.utilities.convertToMessages
import timber.log.Timber
import java.util.concurrent.Executor

/**
 * This boundary callback gets notified when user reaches to the edges of the list such that the
 * database cannot provide any more data.
 * <p>
 * The boundary callback might be called multiple times for the same direction so it does its own
 * rate limiting using the PagingRequestHelper class.
 */
class MessageBoundaryCallback(
    private val dialogId: String,
    private val chatMessageApi: ChatMessageApi,
    private val handleResponse: (List<Message>?) -> Unit,
    private val ioExecutor: Executor,
    private val networkPageSize: Int
) : PagedList.BoundaryCallback<ConnectycubeChatMessage>() {

    val helper = PagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        Timber.d("onZeroItemsLoaded")
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            chatMessageApi.getTop(
                dialogId = dialogId,
                limit = networkPageSize
            )
                .performAsync(createWebserviceCallback(it))
        }
    }

    /**
     * User reached to the end of the list.
     */
    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: ConnectycubeChatMessage) {
        Timber.d("onItemAtEndLoaded itemAtEnd= ${itemAtEnd.body}")
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            chatMessageApi.getTopBefore(
                dialogId = dialogId,
                before = itemAtEnd.dateSent,
                limit = networkPageSize
            )
                .performAsync(createWebserviceCallback(it))
        }
    }

    /**
     * every time it gets new items, boundary callback simply inserts them into the database and
     * paging library takes care of refreshing the list if necessary.
     */
    private fun insertItemsIntoDb(
        response: List<Message>,
        it: PagingRequestHelper.Request.Callback
    ) {
        ioExecutor.execute {
            handleResponse(response)
            it.recordSuccess()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: ConnectycubeChatMessage) {
        // ignored, since we only ever append to what's in the DB
        Timber.d("onItemAtFrontLoaded= ${itemAtFront.body}")
        helper.runIfNotRunning(PagingRequestHelper.RequestType.BEFORE) {
            chatMessageApi.getTopAfter(
                dialogId = dialogId,
                after = itemAtFront.dateSent,
                limit = networkPageSize
            )
                .performAsync(createWebserviceCallback(it))
        }
    }

    private fun createWebserviceCallback(it: PagingRequestHelper.Request.Callback)
            : EntityCallback<ArrayList<ConnectycubeChatMessage>> {
        return object : EntityCallback<ArrayList<ConnectycubeChatMessage>> {
            override fun onSuccess(items: ArrayList<ConnectycubeChatMessage>, bundle: Bundle?) {
                if (items.isNotEmpty()) insertItemsIntoDb(convertToMessages(items), it)
                Timber.d("createWebserviceCallback ${items.size}")
            }

            override fun onError(ex: ResponseException) {
                it.recordFailure(ex)
            }
        }
    }
}