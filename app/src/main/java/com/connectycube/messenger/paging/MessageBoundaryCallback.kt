package com.connectycube.messenger.paging

import androidx.annotation.MainThread
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import com.connectycube.messenger.api.ChatMessageApi
import com.connectycube.messenger.data.Message
import com.connectycube.messenger.utilities.convertToMessages
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.connectycube.chat.models.ConnectycubeMessage
import com.connectycube.core.utils.coroutineDispatcher
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
) : PagedList.BoundaryCallback<ConnectycubeMessage>() {

    val helper = PagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        Timber.d("onZeroItemsLoaded")
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            GlobalScope.apply {
                launch(coroutineDispatcher) {
                    try {
                        val items = chatMessageApi.getTop(
                            dialogId = dialogId,
                            limit = networkPageSize
                        )
                        insertItemsIntoDb(convertToMessages(items), it)
                    } catch (ex: Exception) {
                        it.recordFailure(ex)
                    }
                }
            }
        }
    }

    /**
     * User reached to the end of the list.
     */
    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: ConnectycubeMessage) {
        Timber.d("onItemAtEndLoaded itemAtEnd= ${itemAtEnd.body}")
        helper.runIfNotRunning(PagingRequestHelper.RequestType.BEFORE) {
            GlobalScope.apply {
                launch(coroutineDispatcher) {
                    try {
                        val items = chatMessageApi.getTopBefore(
                            dialogId = dialogId,
                            before = itemAtEnd.dateSent?: 0,
                            limit = networkPageSize
                        )
                        insertItemsIntoDb(convertToMessages(items), it)
                    } catch (ex: Exception) {
                        it.recordFailure(ex)
                    }
                }
            }
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

    override fun onItemAtFrontLoaded(itemAtFront: ConnectycubeMessage) {
        Timber.d("onItemAtFrontLoaded= ${itemAtFront.body}")
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            GlobalScope.apply {
                launch(coroutineDispatcher) {
                    try {
                        val items = chatMessageApi.getTopAfter(
                            dialogId = dialogId,
                            after = itemAtFront.dateSent?: 0,
                            limit = networkPageSize
                        )
                        insertItemsIntoDb(convertToMessages(items), it)
                    } catch (ex: Exception) {
                        it.recordFailure(ex)
                    }
                }
            }
        }
    }
}