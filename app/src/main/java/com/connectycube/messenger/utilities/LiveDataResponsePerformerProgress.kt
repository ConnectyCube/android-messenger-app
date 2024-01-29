package com.connectycube.messenger.utilities

import androidx.lifecycle.MutableLiveData
import com.connectycube.messenger.api.ApiResponse

class LiveDataResponsePerformerProgress<T, R> : LiveDataResponsePerformer<T, R>() {

    lateinit var progressCallBack:((Int) -> Unit)

    override fun perform(performer: suspend () -> T, converter: Converter<R, T>): MutableLiveData<ApiResponse<R>> {
        val liveData = super.perform(performer, converter)
        progressCallBack = { liveData.postValue(ApiResponse.create(it)) }
        return liveData
    }
}
