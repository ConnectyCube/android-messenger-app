package com.connectycube.messenger.utilities

import androidx.lifecycle.MutableLiveData
import com.connectycube.core.ConnectycubeProgressCallback
import com.connectycube.core.server.Performer
import com.connectycube.messenger.api.ApiResponse

class LiveDataResponsePerformerProgress<T, R> : LiveDataResponsePerformer<T, R>() {

    lateinit var progressCallBack: ConnectycubeProgressCallback

    override fun perform(performer: Performer<T>, converter: Converter<R, T>): MutableLiveData<ApiResponse<R>> {
        val liveData = super.perform(performer, converter)
        progressCallBack = ConnectycubeProgressCallback { liveData.postValue(ApiResponse.create(it)) }
        return liveData
    }
}
