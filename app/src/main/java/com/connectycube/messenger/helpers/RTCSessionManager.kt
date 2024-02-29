package com.connectycube.messenger.helpers

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import com.connectycube.messenger.CallActivity
import com.connectycube.messenger.EXTRA_IS_INCOMING_CALL
import com.connectycube.messenger.R
import com.connectycube.messenger.api.ConnectycubePushSender
import org.json.JSONObject
import com.connectycube.ConnectyCube
import com.connectycube.core.utils.ConnectycubeEnvironment.DEVELOPMENT
import com.connectycube.core.utils.NotificationType.PUSH
import com.connectycube.pushnotifications.queries.CreateEventParams
import com.connectycube.webrtc.P2PCalls
import com.connectycube.webrtc.P2PSession
import com.connectycube.webrtc.WebRTCConfig
import com.connectycube.webrtc.WebRTCMediaConfig
import com.connectycube.webrtc.callbacks.RTCCallSessionCallback
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
    private var sessionCallbackListener: RTCCallSessionCallback? = null
    var currentCall: P2PSession? = null

    fun init(applicationContext: Context) {
        this.applicationContext = applicationContext
        this.sessionCallbackListener = RTCSessionCallbackListenerSimple()

        WebRTCConfig.maxOpponentsCount = MAX_OPPONENTS

        ConnectyCube.p2pCalls.addSessionCallbacksListener(sessionCallbackListener!!)
    }

    fun startCall(rtcSession: P2PSession) {
        checkNotNull(applicationContext) { "RTCSessionManager should be initialized before start call" }

        currentCall = rtcSession

        initRTCMediaConfig()
        startCallActivity(false)

        sendCallPushNotification(rtcSession.getOpponents(), rtcSession.getSessionId(), WebRTCConfig.answerTimeInterval)
    }

    private fun initRTCMediaConfig() {
        if (currentCall != null) {
            if (currentCall!!.getOpponents().size < 2) {
                WebRTCMediaConfig.videoWidth = WebRTCMediaConfig.VideoQuality.HD_VIDEO.width
                WebRTCMediaConfig.videoHeight = WebRTCMediaConfig.VideoQuality.HD_VIDEO.height
            } else {
                WebRTCMediaConfig.videoWidth = WebRTCMediaConfig.VideoQuality.QVGA_VIDEO.width
                WebRTCMediaConfig.videoHeight = WebRTCMediaConfig.VideoQuality.QVGA_VIDEO.height
            }
        }
    }

    private fun sendCallPushNotification(
        opponents: List<Int>,
        sessionId: String,
        answerTimeInterval: Long
    ) {
        val cubeEventParams = CreateEventParams()
        cubeEventParams.usersIds.addAll(opponents)
        cubeEventParams.environment = DEVELOPMENT
        cubeEventParams.notificationType = PUSH

        cubeEventParams.parameters[PARAM_MESSAGE] =
            applicationContext?.getString(R.string.you_have_got_new_incoming_call_open_app_to_manage_it) as String
        cubeEventParams.parameters[PARAM_NOTIFICATION_TYPE] = NOTIFICATION_TYPE_CALL
        cubeEventParams.parameters[PARAM_CALL_ID] = sessionId
        cubeEventParams.parameters[PARAM_ANSWER_TIMEOUT] = answerTimeInterval

        val event = cubeEventParams.getEventForRequest()

        ConnectycubePushSender().sendCallPushEvent(event)
    }

    fun receiveCall(rtcSession: P2PSession) {
        if (currentCall != null) {
            if (currentCall!!.getSessionId() != rtcSession.getSessionId()) {
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
        P2PCalls.removeSessionCallbacksListener(sessionCallbackListener!!)

        applicationContext = null
        sessionCallbackListener = null
    }

    private inner class RTCSessionCallbackListenerSimple : RTCCallSessionCallback {
        override fun onCallAcceptByUser(session: P2PSession, opponentId: Int, userInfo: Map<String, String?>?) {}

        override fun onCallRejectByUser(session: P2PSession, opponentId: Int, userInfo: Map<String, String?>?) {}

        override fun onReceiveHangUpFromUser(session: P2PSession, opponentId: Int, userInfo: Map<String, String?>?) {}

        override fun onReceiveNewSession(session: P2PSession) {
            receiveCall(session)
        }

        override fun onSessionClosed(session: P2PSession) {
            if (currentCall == null) return

            if (currentCall!!.getSessionId() == session.getSessionId()) {
                endCall()
            }
        }

        override fun onSessionStartClose(session: P2PSession) {}

        override fun onUserNoActions(session: P2PSession, userId: Int?) {}

        override fun onUserNotAnswer(session: P2PSession, opponentId: Int) {}
    }
}