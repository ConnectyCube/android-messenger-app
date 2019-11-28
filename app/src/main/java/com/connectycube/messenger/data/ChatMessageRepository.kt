package com.connectycube.messenger.data

import android.content.Context
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.api.ChatMessageApi
import com.connectycube.messenger.paging.Listing
import com.connectycube.messenger.paging.MessageBoundaryCallback
import com.connectycube.messenger.paging.NetworkState
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.vo.AppExecutors
import timber.log.Timber
import java.util.*


class ChatMessageRepository(
    private val db: AppDatabase,
    private val chatMessageApi: ChatMessageApi = ChatMessageApi(),
    private val appExecutors: AppExecutors = AppExecutors(),
    private val networkPageSize: Int = DEFAULT_NETWORK_PAGE_SIZE
) {

    fun insertItemIntoDb(item: Message?) {
        item?.let {
            appExecutors.diskIO().execute {
                db.runInTransaction {
                    Timber.d("insertResultIntoDb items= $item")
                    val attachments = convertToListAttachment(item)
                    attachments?.let {
                        db.attachmentDao().insertAndDeleteInTransaction(item.messageId, it)
                    }
                    db.messageDao().insert(item)
                }
            }
        }
    }

    fun updateItemDeliveredStatus(itemId: String, userId: Int) {
        appExecutors.diskIO().execute {
            db.runInTransaction {
                Timber.d("updateItemDeliveredStatus itemId= $itemId, userId= $userId")
                val message: Message? = db.messageDao().loadItem(itemId)
                message?.let {
                    if (message.deliveredIds != null) message.deliveredIds.add(userId)
                    else message.deliveredIds = mutableListOf(userId)

                    db.messageDao().update(message)
                }
            }
        }
    }

    fun updateItemSentStatus(itemId: String, userId: Int) {
        appExecutors.diskIO().execute {
            db.runInTransaction {
                Timber.d("updateItemSentStatus itemId= $itemId, userId= $userId")
                val message: Message? = db.messageDao().loadItem(itemId)
                message?.let {
                    val needUpdate = message.readIds == null &&
                            (message.deliveredIds == null || !message.deliveredIds.contains(userId))
                    Timber.d("updateItemSentStatus needUpdate= $needUpdate")
                    if (needUpdate) {
                        if (message.deliveredIds != null) message.deliveredIds.add(userId)
                        else message.deliveredIds = mutableListOf(userId)
                        db.messageDao().update(message)
                    }
                }
            }
        }
    }

    fun updateItemReadStatus(itemId: String, userId: Int) {
        appExecutors.diskIO().execute {
            db.runInTransaction {
                Timber.d("updateItemReadStatus itemId= $itemId, userId= $userId")
                val message: Message? = db.messageDao().loadItem(itemId)
                message?.let {
                    if (message.readIds != null && message.readIds.contains(userId)) return@runInTransaction
                    if (message.readIds == null) message.readIds = mutableListOf(userId)
                    else message.readIds.add(userId)
                    db.messageDao().update(message)
                }
            }
        }
    }

    /**
     * Inserts the response into the database while also assigning position indices to items.
     */
    private fun insertResultIntoDb(items: List<Message>?) {
        items?.isNotEmpty().let {
            db.runInTransaction {
                Timber.d("insertResultIntoDb items= ${items?.size}, items= $items")
                items?.let {
                    val attachments = convertToListOfListMessages(items)
                    if (attachments.isNotEmpty()) db.attachmentDao().insert(attachments)
                    db.messageDao().insert(items)
                }
            }
        }
    }

    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(dialogId: String): LiveData<NetworkState> {
        Timber.d("refresh")
        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.LOADING
        chatMessageApi.getTop(dialogId, networkPageSize).performAsync(
            object : EntityCallback<ArrayList<ConnectycubeChatMessage>> {
                override fun onSuccess(list: ArrayList<ConnectycubeChatMessage>, bundle: Bundle) {
                    appExecutors.networkIO().execute {
                        db.runInTransaction {
                            db.messageDao().deleteByDialogId(dialogId)
                            insertResultIntoDb(convertToMessages(list))
                        }
                        // since we are in bg thread now, post the result.
                        networkState.postValue(NetworkState.LOADED)
                    }
                }

                override fun onError(ex: ResponseException) {
                    networkState.value = NetworkState.error(ex.message)
                }
            }
        )
        return networkState
    }

    @MainThread
    fun postsOfDialogId(dialogId: String, pageSize: Int): Listing<ConnectycubeChatMessage> {
        // create a boundary callback which will observe when the user reaches to the edges of
        // the list and update the database with extra data.
        val boundaryCallback = MessageBoundaryCallback(
            chatMessageApi = chatMessageApi,
            dialogId = dialogId,
            handleResponse = this::insertResultIntoDb,
            ioExecutor = appExecutors.diskIO(),
            networkPageSize = networkPageSize
        )
        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh(dialogId)
        }

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(pageSize)
            .build()

        val dataSourceConnectycubeChatMessage: DataSource.Factory<Int, ConnectycubeChatMessage> =
            db.messageWithAttachmentDao().postsByDialogId(dialogId).map { it.message }

//        val livePagedList: LiveData<PagedList<ConnectycubeChatMessage>>
//                = LivePagedListBuilder(db.messageWithAttachmentDao().postsByDialogId(dialogId).map { it.message as ConnectycubeChatMessage }, config).setBoundaryCallback(boundaryCallback).build()
        val livePagedList = dataSourceConnectycubeChatMessage.toLiveData(
            pageSize = pageSize,
            boundaryCallback = boundaryCallback
        )

        Timber.d("livePagedList= $livePagedList")
        return Listing(
            pagedList = livePagedList,
            networkState = boundaryCallback.networkState,
            retry = {
                boundaryCallback.helper.retryAllFailed()
            },
            refresh = {
                refreshTrigger.value = null
            },
            refreshState = refreshState
        )
    }

    companion object {
        private const val DEFAULT_NETWORK_PAGE_SIZE = 20
        // For Singleton instantiation
        @Volatile
        private var instance: ChatMessageRepository? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance
                    ?: ChatMessageRepository(AppDatabase.getInstance(context.applicationContext)).also {
                        instance = it
                    }
            }
    }
}