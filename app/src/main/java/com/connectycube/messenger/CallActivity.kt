package com.connectycube.messenger

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.helpers.RingtoneManager
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.viewmodels.CallViewModel
import com.connectycube.webrtc.*
import com.connectycube.webrtc.callbacks.RTCCallSessionCallback
import kotlinx.android.synthetic.main.activity_call.*
import timber.log.Timber
import com.connectycube.webrtc.callbacks.RTCSessionStateCallback


const val EXTRA_IS_INCOMING_CALL = "conversation_type"

class CallActivity : AppCompatActivity(R.layout.activity_call), RTCCallSessionCallback,
    RTCSessionStateCallback<P2PSession> {

    private val callViewModel: CallViewModel by viewModels {
        InjectorUtils.provideCallViewModelFactory(this.application)
    }
    private val permissionsHelper = PermissionsHelper(this)
    private lateinit var ringtoneManager: RingtoneManager
    private var currentSession: P2PSession? = null
    private var audioManager: AppRTCAudioManager? = null
    private var isInComingCall: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSession()
        initFields()
        initToolbar()
        initCall()
        initAudioManager()
        initRingtoneManager()
        checkPermissionsAndProceed()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun initSession() {
        currentSession = RTCSessionManager.getInstance().currentCall
        currentSession?.addSessionStateCallbacksListener(this)
        currentSession?.initSignallingWithOpponents()
    }

    private fun initFields() {
        isInComingCall = intent?.extras!!.getBoolean(EXTRA_IS_INCOMING_CALL)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        toggle_speaker.setOnClickListener { switchAudioDevice() }
        toggle_mute_mic.setOnClickListener { setMuteAudio(toggle_mute_mic.isChecked) }
        toggle_screen_sharing.setOnClickListener { screenSharing() }
        updateToolbar()
    }

    private fun checkPermissionsAndProceed() {
        if (permissionsHelper.areCallPermissionsGranted()) {
            startFragment()
        } else {
            permissionsHelper.requestCallPermissions()
        }
    }

    private fun updateToolbar(showFull: Boolean = false) {
        currentSession?.let {
            if (isInComingCall && !showFull) {
                toggle_speaker.visibility = View.INVISIBLE
                toggle_mute_mic.visibility = View.INVISIBLE
                toggle_screen_sharing.visibility = View.INVISIBLE
            } else {
                if (it.getCallType() == CallType.AUDIO) {
                    toggle_mute_mic.visibility = View.VISIBLE
                    toggle_speaker.visibility = View.VISIBLE
                    toggle_screen_sharing.visibility = View.GONE
                } else {
                    toggle_screen_sharing.visibility = View.VISIBLE
                    toggle_mute_mic.visibility = View.GONE
                    toggle_speaker.visibility = View.GONE
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

    private fun setMuteAudio(isEnabled: Boolean) {
        currentSession?.apply {
            mediaStreamManager?.localAudioTrack?.enabled = isEnabled
        }
    }

    private fun screenSharing() {
        Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_LONG).show()
    }

    private fun initCall() {
        P2PCalls.addSessionCallbacksListener(this)
    }

    private fun initAudioManager() {
        if (audioManager == null) {
            audioManager = AppRTCAudioManager.create(this)
            audioManager?.apply {
                defaultAudioDevice = AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
                setOnWiredHeadsetStateListener(object :
                                                   AppRTCAudioManager.OnWiredHeadsetStateListener {
                    override fun onWiredHeadsetStateChanged(plugged: Boolean,
                                                            hasMicrophone: Boolean
                    ) {
                        Timber.d("plugged= $plugged, hasMicrophone= $hasMicrophone")
                    }

                })
                setBluetoothAudioDeviceStateListener(object :
                                                         AppRTCAudioManager.BluetoothAudioDeviceStateListener {
                    override fun onStateChanged(connected: Boolean) {
                        Timber.d("connected= $connected")
                    }

                })
            }
        }
    }

    private fun startAudioManager() {
        audioManager?.start(object : AppRTCAudioManager.AudioManagerEvents {
            override fun onAudioDeviceChanged(selectedAudioDevice: AppRTCAudioManager.AudioDevice?,
                                              availableAudioDevices: Set<AppRTCAudioManager.AudioDevice?>?
            ) {
                Timber.d("Audio device switched to  $selectedAudioDevice")
            }

        })
    }

    private fun initRingtoneManager() {
        ringtoneManager = RingtoneManager(this, R.raw.ringtone_outgoing)
    }

    private fun startFragment() {
        if (isInComingCall) {
            startIncomingCallFragment()
            subscribeIncomingScreen()
        } else {
            ringtoneManager.start()
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
            CallType.VIDEO == currentSession?.getCallType()
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

    private fun rejectCurrentSession() {
        currentSession?.rejectCall(HashMap<String, String>())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_CALL -> {
                if (permissionsHelper.areCallPermissionsGranted()) {
                    Timber.d("permission was granted")
                    startFragment()
                } else {
                    Timber.d("permission was denied")
                    Toast.makeText(
                        this,
                        getString(
                            R.string.denied_permission,
                            permissions.joinToString()
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onSessionStartClose(session: P2PSession) {
        if (session == currentSession) {
            currentSession?.removeSessionStateCallbacksListener(this@CallActivity)
            callViewModel.callSessionAction.value = CallViewModel.CallSessionAction.CALL_STOPPED
        }
    }

    override fun onReceiveNewSession(session: P2PSession) {
        Timber.d("onReceiveNewSession")
        if (currentSession != null) {
            Timber.d("reject new session, device is busy")
            session.rejectCall(null)
        }
    }

    override fun onUserNoActions(session: P2PSession, userId: Int?) {
    }

    override fun onCallAcceptByUser(session: P2PSession,
                                    opponentId: Int,
                                    userInfo: Map<String, String?>?
    ) {
        if (session != currentSession) {
            return
        }
        ringtoneManager.stop()
    }

    override fun onCallRejectByUser(session: P2PSession,
                                    opponentId: Int,
                                    userInfo: Map<String, String?>?
    ) {
    }

    override fun onReceiveHangUpFromUser(session: P2PSession,
                                         opponentId: Int,
                                         userInfo: Map<String, String?>?
    ) {
        Timber.d("onReceiveHangUpFromUser userId= $opponentId")
    }

    override fun onSessionClosed(session: P2PSession) {
        Timber.d("onSessionClosed session= $session")
        if (session == currentSession) {
            Timber.d("release currentSession")
            releaseCurrentCall()
            ringtoneManager.stop()
            finish()
        }
    }

    override fun onUserNotAnswer(session: P2PSession, opponentId: Int) {
        if (session != currentSession) {
            return
        }
        ringtoneManager.stop()
    }

    private fun hangUpCurrentSession() {
        ringtoneManager.stop()
        currentSession?.hangUp(HashMap<String, String>())
    }

    private fun releaseCurrentCall() {
        audioManager?.stop()
        currentSession?.removeSessionStateCallbacksListener(this)
        P2PCalls.removeSessionCallbacksListener(this)
        currentSession = null
        RTCSessionManager.getInstance().endCall()
    }

    override fun onDisconnectedFromUser(session: P2PSession, userId: Int) {
    }

    override fun onConnectedToUser(session: P2PSession, userId: Int) {
        callViewModel.callSessionAction.value = CallViewModel.CallSessionAction.CALL_STARTED
    }

    override fun onConnectionClosedForUser(session: P2PSession, userId: Int) {
    }

    override fun onStateChanged(session: P2PSession, state: BaseSession.RTCSessionState) {
    }
}