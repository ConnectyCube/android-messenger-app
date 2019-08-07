package com.connectycube.messenger.paging

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import java.util.concurrent.Executor

class SubConnectycubeDataSourceFactory(
    private val dialog: ConnectycubeChatDialog,
    private val retryExecutor: Executor
) : DataSource.Factory<Int, ConnectycubeChatMessage>() {
    val sourceLiveData = MutableLiveData<PageKeyedConnectycubeDataSource>()
    override fun create(): DataSource<Int, ConnectycubeChatMessage> {
        val source = PageKeyedConnectycubeDataSource(dialog, retryExecutor)
        sourceLiveData.postValue(source)
        return source
    }
}