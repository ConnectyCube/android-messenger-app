package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.connectycube.messenger.R
import com.connectycube.messenger.data.UserRepository
import com.connectycube.messenger.vo.Resource
import com.connectycube.users.model.ConnectycubeUser

class CallViewModel internal constructor(
    applicationContext: Application,
    private val userRepo: UserRepository
) :
    AndroidViewModel(applicationContext) {


    val incomingCallAction = MutableLiveData<CallUserAction>()
    val callUserAction = MutableLiveData<CallUserAction>()
    val callSessionAction = MutableLiveData<CallSessionAction>()

    enum class CallUserAction {
        ACCEPT, REJECT, HANGUP
    }

    enum class CallSessionAction {
        CALL_STARTED, CALL_STOPPED, SWITCHED_CAMERA
    }


    fun getOpponents(vararg opponentsIds: Int): LiveData<Resource<List<ConnectycubeUser>>> {
        val result = MediatorLiveData<Resource<List<ConnectycubeUser>>>()
        result.value = Resource.loading(null)

        val source = userRepo.getUsersByIds(*opponentsIds)
        result.addSource(source) {
            if (it.isNullOrEmpty()) {
                result.value = Resource.error(
                    getApplication<Application>().getString(R.string.error_while_loading_users),
                    null)
            } else {
                result.value = Resource.success(it
                    .map { user -> user.conUser })
                result.removeSource(source)
            }
        }

        return result
    }



}