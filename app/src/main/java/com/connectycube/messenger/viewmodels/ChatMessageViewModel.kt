package com.connectycube.messenger.viewmodels

import androidx.lifecycle.*
import androidx.lifecycle.Transformations.map
import androidx.paging.PagedList
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.data.ChatMessageRepository

private const val PAGE_SIZE = 20

class ChatMessageViewModel internal constructor(val repository: ChatMessageRepository, val chat: ConnectycubeChatDialog):
    ViewModel()  {
    private var scroll: Boolean = false
    private val dialogName = createShowDialog()
    private val repoResult = map(dialogName) {
        repository.loadDialogMessages(it, PAGE_SIZE)
    }
    val messages = Transformations.switchMap(repoResult) {
        MediatorLiveData<Pair<PagedList<ConnectycubeChatMessage>, Boolean>>().apply {
            addSource(it.pagedList) {data ->
                value = Pair(data, scroll)
            }
        }}
    val networkState = Transformations.switchMap(repoResult, { it.networkState })!!
    val refreshState = Transformations.switchMap(repoResult, { it.refreshState })!!

    fun refresh(scroll: Boolean = false) {
        this.scroll = scroll
        repoResult.value?.refresh?.invoke()
    }

    private fun createShowDialog():MutableLiveData<ConnectycubeChatDialog> {
        val dialogName = MutableLiveData<ConnectycubeChatDialog>()
        dialogName.value = chat
        return dialogName
    }

    fun retry() {
        val listing = repoResult.value
        listing?.retry?.invoke()
    }

    fun currentDialogName(): ConnectycubeChatDialog? = dialogName.value
}