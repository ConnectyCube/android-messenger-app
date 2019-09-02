package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.messenger.data.AvatarRepository
import com.connectycube.messenger.vo.Resource
import com.connectycube.messenger.vo.Status

class CreateDialogDetailsViewModel internal constructor(
    applicationContext: Application,
    private val avatarRepository: AvatarRepository
) : AndroidViewModel(applicationContext) {
    var photoUrl: String? = null

    val liveDataResult = MediatorLiveData<Resource<String>>()

    fun uploadAvatar(path: String): LiveData<Resource<String>> {
        val source = avatarRepository.uploadFile(path)
        liveDataResult.addSource(source) {
            liveDataResult.value = it
            if (it.status == Status.SUCCESS) {
                photoUrl = it.data
                liveDataResult.removeSource(source)
            }
        }
        return liveDataResult
    }
}