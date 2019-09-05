package com.connectycube.messenger.helpers

import android.content.Context
import android.content.Intent
import com.connectycube.videochat.RTCSession
import timber.log.Timber

class RTCSessionManager private constructor(val context: Context) { //TODO VT delete context from constructor
    private var currentCall: RTCSession? = null

    fun startCall(rtcSession: RTCSession){
        currentCall = rtcSession

        rtcSession.startCall(hashMapOf())
        startCallActivity(false)
    }

    fun receiveCall(rtcSession: RTCSession){
        if (currentCall != null) return

        currentCall = rtcSession
        startCallActivity(true)
    }

    fun endCall(){
        currentCall = null
    }

    private fun startCallActivity(isIncomig: Boolean){
        Timber.w("start call incoming - $isIncomig")

//        val intent = Intent(context, CallActivity::class.java)
//        intent.putExtra(EXTRA_CALL_DIRECTION, isIncomig)

//        context.startActivity(intent)
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: RTCSessionManager? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: RTCSessionManager(context).also { instance = it }
            }
    }
}