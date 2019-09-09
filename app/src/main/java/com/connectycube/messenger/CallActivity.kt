package com.connectycube.messenger

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.WebRTCSignaling
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.observeOnce
import com.connectycube.messenger.viewmodels.CallViewModel
import com.connectycube.videochat.*
import com.connectycube.videochat.callbacks.RTCClientSessionCallbacks
import com.connectycube.videochat.callbacks.RTCSessionEventsCallback
import com.connectycube.videochat.callbacks.RTCSessionStateCallback
import kotlinx.android.synthetic.main.toolbar_call_activity.*
import timber.log.Timber
import java.util.HashMap


const val MAX_OPPONENTS = 4
const val EXTRA_IS_INCOMING_CALL = "conversation_type"

class CallActivity : AppCompatActivity(R.layout.activity_call), RTCClientSessionCallbacks,
    RTCSessionEventsCallback, RTCSessionStateCallback<RTCSession> {

    private val callViewModel: CallViewModel by viewModels {
        InjectorUtils.provideCallViewModelFactory(this.application)
    }
    private var currentSession: RTCSession? = null
    private var audioManager: AppRTCAudioManager? = null
    private var isInComingCall: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSession()
        initFields()
        initToolbar()
        initCall()
        initAudioManager()
        startFragment()
    }

    private fun initSession() {
        currentSession = RTCSessionManager.getInstance(this).currentCall
        currentSession?.addSessionCallbacksListener(this@CallActivity)
    }

    private fun initFields() {
        isInComingCall = intent?.extras!!.getBoolean(EXTRA_IS_INCOMING_CALL)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        toggle_speaker.setOnClickListener { switchAudioDevice() }
        toggle_mute.setOnClickListener { setAudioMute(toggle_mute.isChecked) }
        updateToolbar()
    }

    private fun updateToolbar(showFull: Boolean = false) {
        currentSession?.let {
            if (isInComingCall && !showFull) {
                toggle_speaker.visibility = View.INVISIBLE
                toggle_mute.visibility = View.INVISIBLE
            } else {
                toggle_mute.visibility = View.VISIBLE
                if (it.isAudioCall) {
                    toggle_speaker.visibility = View.VISIBLE
                } else {
                    toggle_speaker.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun switchAudioDevice() {
        audioManager?.apply {
            if (selectedAudioDevice != AppRTCAudioManager.AudioDevice.SPEAKER_PHONE) {
                selectAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)
            } else {
                when {
                    audioDevices.contains(AppRTCAudioManager.AudioDevice.BLUETOOTH) -> selectAudioDevice(
                        AppRTCAudioManager.AudioDevice.BLUETOOTH
                    )
                    audioDevices.contains(AppRTCAudioManager.AudioDevice.WIRED_HEADSET) -> selectAudioDevice(
                        AppRTCAudioManager.AudioDevice.WIRED_HEADSET
                    )
                    else -> selectAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE)
                }
            }
        }
    }

    private fun setAudioMute(isEnabled: Boolean) {
        currentSession?.apply {
            mediaStreamManager?.localAudioTrack?.setEnabled(isEnabled)
        }
    }

    private fun initCall() {
        ConnectycubeChatService.getInstance().videoChatWebRTCSignalingManager?.addSignalingManagerListener { signaling, createdLocally ->
            if (!createdLocally) {
                RTCClient.getInstance(this).addSignaling(signaling as WebRTCSignaling)
            }
        }

        // Configure
        //
        RTCConfig.setMaxOpponentsCount(MAX_OPPONENTS)
        RTCConfig.setDebugEnabled(true)
        RTCClient.getInstance(this).addSessionCallbacksListener(this)
        RTCClient.getInstance(this).prepareToProcessCalls()
    }

    private fun initAudioManager() {
        if (audioManager == null) {
            audioManager = AppRTCAudioManager.create(this)
            audioManager?.apply {
                defaultAudioDevice = AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
                setOnWiredHeadsetStateListener { plugged, hasMicrophone ->
                    Timber.d("plugged= $plugged, hasMicrophone= $hasMicrophone")
                }
                setBluetoothAudioDeviceStateListener { connected ->
                    Timber.d("connected= $connected")
                }
            }
        }
    }

    private fun startAudioManager() {
        audioManager?.start { selectedAudioDevice, availableAudioDevices ->
            Timber.d("Audio device switched to  $selectedAudioDevice")

        }
    }

    private fun startFragment() {
        if (isInComingCall) {
            initIncomingStopCallTask()
            startIncomingCallFragment()
            subscribeIncomingScreen()
        } else {
            startCall()
        }
    }

    private fun subscribeIncomingScreen() {
        callViewModel.incomingCallAction.observeOnce(this, Observer {
            it?.let {
                when (it) {
                    CallViewModel.CallUserAction.ACCEPT -> {
                        startCall()
                    }
                    CallViewModel.CallUserAction.REJECT -> rejectCurrentSession()
                    else -> Timber.d("subscribeIncomingScreen not defined action $it")
                }
            }
        })
    }

    private fun subscribeCallScreen() {
        callViewModel.callUserAction.observeOnce(this, Observer {
            it?.let {
                when (it) {
                    CallViewModel.CallUserAction.HANGUP -> hangUpCurrentSession()
                    else -> Timber.d("subscribeIncomingScreen not defined action $it")
                }
            }
        })
    }

    private fun startIncomingCallFragment() {
        currentSession?.let {
            val fragment = IncomingCallFragment()
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                fragment,
                fragment::class.java.simpleName
            ).commitAllowingStateLoss()
        }
    }

    private fun startCallFragment() {
        val isVideoCall =
            RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO == currentSession?.conferenceType
        val conversationFragment = BaseCallFragment.createInstance(
            if (isVideoCall) VideoCallFragment()
            else AudioCallFragment(),
            isInComingCall
        )
        supportFragmentManager.beginTransaction().replace(
            R.id.fragment_container,
            conversationFragment,
            conversationFragment::class.java.simpleName
        ).commitAllowingStateLoss()
    }

    private fun startCall() {
        updateToolbar(true)
        startAudioManager()
        startCallFragment()
        subscribeCallScreen()
    }

    fun rejectCurrentSession() {
        currentSession?.rejectCall(HashMap<String, String>())
    }

    private fun initIncomingStopCallTask() {
//        showIncomingCallWindowTaskHandler = Handler(Looper.myLooper())
//        showIncomingCallWindowTask = {
//            if (currentSession == null) {
//                return
//            }
//
//            val currentSessionState = currentSession.getState()
//            if (RTCSession.RTCSessionState.RTC_SESSION_NEW == currentSessionState) {
//                rejectCurrentSession()
//            } else {
//                ringtonePlayer.stop()
//                hangUpCurrentSession()
//            }
//            showToast("Call was stopped by timer")
//        }
    }

    override fun onUserNotAnswer(session: RTCSession?, userId: Int?) {
    }

    override fun onSessionStartClose(session: RTCSession) {
        if (session == currentSession) {
            currentSession?.removeSessionCallbacksListener(this@CallActivity)
            callViewModel.callSessionAction.value = CallViewModel.CallSessionAction.CALL_STOPPED
        }
    }

    override fun onReceiveHangUpFromUser(session: RTCSession?,
                                         userId: Int?,
                                         userInfo: MutableMap<String, String>?
    ) {
        Toast.makeText(
            applicationContext, "User $userId " + getString(R.string.call_status_hang_up),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCallAcceptByUser(session: RTCSession?,
                                    userId: Int?,
                                    data: MutableMap<String, String>?
    ) {
    }

    override fun onReceiveNewSession(session: RTCSession) {
        Timber.d("onReceiveNewSession")
        if (currentSession == null) {
            initCurrentSession(session)
        } else {
            Timber.d("Stop new session. Device now is busy")
            session.rejectCall(null)
        }
    }

    private fun initCurrentSession(session: RTCSession) {
        Timber.d("Init new RTCSession")
        currentSession = session
    }


    override fun onUserNoActions(session: RTCSession?, userId: Int?) {
    }

    override fun onSessionClosed(session: RTCSession) {
        Timber.d("onSessionClosed session= $session")
        if (session == currentSession) {
            Timber.d("release currentSession")
            releaseCurrentCall()
            finish()
        }
    }

    override fun onCallRejectByUser(session: RTCSession?,
                                    userId: Int?,
                                    data: MutableMap<String, String>?
    ) {
    }

    fun hangUpCurrentSession() {
        currentSession?.hangUp(HashMap<String, String>())
    }

    private fun releaseCurrentCall() {
        audioManager?.stop()
        currentSession = null
        RTCSessionManager.getInstance(applicationContext).endCall()
    }

    override fun onDisconnectedFromUser(session: RTCSession?, userID: Int?) {
    }

    override fun onConnectedToUser(session: RTCSession?, userID: Int?) {
        callViewModel.callSessionAction.value = CallViewModel.CallSessionAction.CALL_STARTED
    }

    override fun onConnectionClosedForUser(session: RTCSession?, userID: Int?) {
    }

    override fun onStateChanged(session: RTCSession?, state: BaseSession.RTCSessionState?) {
    }
}