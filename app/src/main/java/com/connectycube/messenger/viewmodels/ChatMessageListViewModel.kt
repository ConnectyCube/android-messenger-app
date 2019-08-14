package com.connectycube.messenger.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.data.ChatMessageRepository
import com.connectycube.messenger.utilities.convertToMessage
import java.util.concurrent.atomic.AtomicBoolean

private const val PAGE_SIZE = 5

class ChatMessageListViewModel internal constructor(
    private val repository: ChatMessageRepository,
    private val chat: ConnectycubeChatDialog
) :
    ViewModel() {
    private var scroll: AtomicBoolean = AtomicBoolean(false)
    private val dialogName = createShowDialog()
    private val repoResult = map(dialogName) {
        repository.postsOfDialogId(it.dialogId, PAGE_SIZE)
    }

    val networkState = Transformations.switchMap(repoResult, { it.networkState })
    val messages = Transformations.switchMap(repoResult, { it.pagedList })
    val refreshState = Transformations.switchMap(repoResult, { it.refreshState })

    private fun createShowDialog(): MutableLiveData<ConnectycubeChatDialog> {
        val dialogName = MutableLiveData<ConnectycubeChatDialog>()
        dialogName.value = chat
        return dialogName
    }

    fun postItem(message: ConnectycubeChatMessage, scroll: Boolean = false) {
        this.scroll.set(scroll)
        repository.insertItemIntoDb(convertToMessage(message))
    }

    fun getScroll(): Boolean {
        return scroll.getAndSet(false)
    }

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun retry() {
        val listing = repoResult.value
        listing?.retry?.invoke()
    }
}
