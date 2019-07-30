package com.connectycube.messenger.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.data.ChatMessageRepository

private const val PAGE_SIZE = 20

class ChatMessageViewModel internal constructor(val repository: ChatMessageRepository, val chat: ConnectycubeChatDialog):
    ViewModel()  {
    private val dialogName = createShowDialog()
    private val repoResult = map(dialogName) {
        repository.loadDialogMessages(it, PAGE_SIZE)
    }

    val messages = Transformations.switchMap(repoResult, { it.pagedList })!!
    val networkState = Transformations.switchMap(repoResult, { it.networkState })!!
    val refreshState = Transformations.switchMap(repoResult, { it.refreshState })!!

    fun refresh() {
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