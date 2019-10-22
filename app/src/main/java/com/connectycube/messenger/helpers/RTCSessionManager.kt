package com.connectycube.messenger.helpers

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.WebRTCSignaling
import com.connectycube.core.helper.StringifyArrayList
import com.connectycube.messenger.CallActivity
import com.connectycube.messenger.EXTRA_IS_INCOMING_CALL
import com.connectycube.messenger.R
import com.connectycube.messenger.api.ConnectycubePushSender
import com.connectycube.pushnotifications.model.ConnectycubeEnvironment
import com.connectycube.pushnotifications.model.ConnectycubeEvent
import com.connectycube.pushnotifications.model.ConnectycubeNotificationType
import com.connectycube.videochat.RTCClient
import com.connectycube.videochat.RTCConfig
import com.connectycube.videochat.RTCMediaConfig
import com.connectycube.videochat.RTCSession
import com.connectycube.videochat.callbacks.RTCClientSessionCallbacks
import com.connectycube.videochat.callbacks.RTCClientSessionCallbacksImpl
import org.json.JSONObject
import timber.log.Timber

const val MAX_OPPONENTS = 4

class RTCSessionManager {

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: RTCSessionManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: RTCSessionManager().also { instance = it }
            }
    }

    private var applicationContext: Context? = null
    private var sessionCallbackListener: RTCClientSessionCallbacks? = null
    var currentCall: RTCSession? = null

    fun init(applicationContext: Context) {
        this.applicationContext = applicationContext
        this.sessionCallbackListener = RTCSessionCallbackListenerSimple()

        RTCConfig.setMaxOpponentsCount(MAX_OPPONENTS)
        RTCConfig.setDebugEnabled(true)

        ConnectycubeChatService.getInstance()
            .videoChatWebRTCSignalingManager?.addSignalingManagerListener { signaling, createdLocally ->
            if (!createdLocally) {
                RTCClient.getInstance(applicationContext).addSignaling(signaling as WebRTCSignaling)
            }
        }

        RTCClient.getInstance(applicationContext).addSessionCallbacksListener(
            sessionCallbackListener
        )
        RTCClient.getInstance(applicationContext).prepareToProcessCalls()
    }

    fun startCall(rtcSession: RTCSession) {
        checkNotNull(applicationContext) { "RTCSessionManager should be initialized before start call" }

        currentCall = rtcSession

        initRTCMediaConfig()
        startCallActivity(false)

        sendCallPushNotification(rtcSession.opponents, rtcSession.sessionID, RTCConfig.getAnswerTimeInterval())
    }

    private fun initRTCMediaConfig() {
        currentCall?.let {
            if (it.opponents.size < 2) {
                RTCMediaConfig.setVideoWidth(RTCMediaConfig.VideoQuality.HD_VIDEO.width)
                RTCMediaConfig.setVideoHeight(RTCMediaConfig.VideoQuality.HD_VIDEO.height)
            } else {
                RTCMediaConfig.setVideoWidth(RTCMediaConfig.VideoQuality.QVGA_VIDEO.width)
                RTCMediaConfig.setVideoHeight(RTCMediaConfig.VideoQuality.QVGA_VIDEO.height)
            }
        }
    }

    private fun sendCallPushNotification(
        opponents: List<Int>,
        sessionId: String,
        answerTimeInterval: Long
    ) {
        val event = ConnectycubeEvent()
        event.userIds = StringifyArrayList(opponents)
        event.environment = ConnectycubeEnvironment.DEVELOPMENT
        event.notificationType = ConnectycubeNotificationType.PUSH

        val json = JSONObject()
        try {
            json.put(
                PARAM_MESSAGE,
                applicationContext?.getString(R.string.you_have_got_new_incoming_call_open_app_to_manage_it)
            )
            // custom parameters
            json.put(PARAM_NOTIFICATION_TYPE, NOTIFICATION_TYPE_CALL)
            json.put(PARAM_CALL_ID, sessionId)
            json.put(PARAM_ANSWER_TIMEOUT, answerTimeInterval)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        event.message = json.toString()

        ConnectycubePushSender().sendCallPushEvent(event)
    }

    fun receiveCall(rtcSession: RTCSession) {
        if (currentCall != null) {
            if (currentCall!!.sessionID != rtcSession.sessionID) {
                rtcSession.rejectCall(hashMapOf())
            }
            return
        }

        currentCall = rtcSession

        initRTCMediaConfig()
        startCallActivity(true)
    }

    fun endCall() {
        currentCall = null
    }

    private fun startCallActivity(isIncoming: Boolean) {
        Timber.w("start call incoming - $isIncoming")

        val intent = Intent(applicationContext, CallActivity::class.java)
        intent.flags = FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(EXTRA_IS_INCOMING_CALL, isIncoming)

        applicationContext?.startActivity(intent)
    }

    fun destroy() {
        RTCClient.getInstance(applicationContext)
            .removeSessionsCallbacksListener(sessionCallbackListener)
        RTCClient.getInstance(applicationContext).destroy()

        applicationContext = null
        sessionCallbackListener = null
    }

    private inner class RTCSessionCallbackListenerSimple : RTCClientSessionCallbacksImpl() {
        override fun onReceiveNewSession(session: RTCSession?) {
            super.onReceiveNewSession(session)
            session?.let { receiveCall(session) }
        }

        override fun onSessionClosed(session: RTCSession?) {
            super.onSessionClosed(session)
            if (session == null || currentCall == null) return

            if (currentCall!!.sessionID == session.sessionID) {
                endCall()
            }
        }
    }
}