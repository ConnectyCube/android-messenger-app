package com.connectycube.messenger

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.helpers.RingtoneManager
import com.connectycube.messenger.helpers.showSnackbar
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.viewmodels.CallViewModel
import com.connectycube.videochat.*
import com.connectycube.videochat.callbacks.RTCClientSessionCallbacks
import com.connectycube.videochat.callbacks.RTCSessionEventsCallback
import com.connectycube.videochat.callbacks.RTCSessionStateCallback
import kotlinx.android.synthetic.main.activity_call.*
import org.jivesoftware.smack.AbstractConnectionListener
import org.webrtc.CameraVideoCapturer
import timber.log.Timber
import com.google.android.material.snackbar.Snackbar


const val EXTRA_IS_INCOMING_CALL = "conversation_type"

class CallActivity : AppCompatActivity(R.layout.activity_call), RTCClientSessionCallbacks,
    RTCSessionEventsCallback, RTCSessionStateCallback<RTCSession> {

    private val callViewModel: CallViewModel by viewModels {
        InjectorUtils.provideCallViewModelFactory(this.application)
    }
    private val permissionsHelper = PermissionsHelper(this)
    private lateinit var ringtoneManager: RingtoneManager
    private var currentSession: RTCSession? = null
    private var audioManager: AppRTCAudioManager? = null
    private val cameraSwitchHandler = CameraSwitchHandler()
    private val connectionListener = ConnectionListener()
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
        ConnectycubeChatService.getInstance().addConnectionListener(connectionListener)
    }

    override fun onStop() {
        super.onStop()
        ConnectycubeChatService.getInstance().removeConnectionListener(connectionListener)
    }

    private fun initSession() {
        currentSession = RTCSessionManager.getInstance().currentCall
        currentSession?.addSessionCallbacksListener(this@CallActivity)
    }

    private fun initFields() {
        isInComingCall = intent?.extras!!.getBoolean(EXTRA_IS_INCOMING_CALL)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        toggle_speaker.setOnClickListener { switchAudioDevice() }
        toggle_mute_mic.setOnClickListener { setAudioMute(toggle_mute_mic.isChecked) }
        toggle_camera.setOnClickListener { switchCamera() }
        toggle_mute_camera.setOnClickListener { setMuteCamera(toggle_mute_camera.isChecked) }
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
                toggle_camera.visibility = View.INVISIBLE
                toggle_mute_camera.visibility = View.INVISIBLE
            } else {
                toggle_mute_mic.visibility = View.VISIBLE
                if (it.isAudioCall) {
                    toggle_speaker.visibility = View.VISIBLE
                    toggle_camera.visibility = View.GONE
                    toggle_mute_camera.visibility = View.GONE
                } else {
                    toggle_speaker.visibility = View.GONE
                    toggle_camera.visibility = View.VISIBLE
                    toggle_mute_camera.visibility = View.VISIBLE
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

    private fun switchCamera() {
        toggle_camera.isEnabled = false
        (currentSession?.mediaStreamManager?.videoCapturer as RTCCameraVideoCapturer)
            .switchCamera(cameraSwitchHandler)
    }

    private fun setMuteCamera(isEnabled: Boolean) {
        currentSession?.apply {
            mediaStreamManager?.localVideoTrack?.setEnabled(isEnabled)
        }
    }

    private fun initCall() {
        RTCClient.getInstance(this).addSessionCallbacksListener(this)
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

    override fun onUserNotAnswer(session: RTCSession?, userId: Int?) {
        if (session != currentSession) {
            return
        }
        ringtoneManager.stop()
    }

    override fun onSessionStartClose(session: RTCSession) {
        if (session == currentSession) {
            currentSession?.removeSessionCallbacksListener(this@CallActivity)
            callViewModel.callSessionAction.value = CallViewModel.CallSessionAction.CALL_STOPPED
        }
    }

    override fun onReceiveHangUpFromUser(session: RTCSession,
                                         userId: Int,
                                         userInfo: MutableMap<String, String>?
    ) {
        Timber.d("onReceiveHangUpFromUser userId= $userId")
    }

    override fun onCallAcceptByUser(session: RTCSession?,
                                    userId: Int?,
                                    data: MutableMap<String, String>?
    ) {
        if (session != currentSession) {
            return
        }
        ringtoneManager.stop()
    }

    override fun onReceiveNewSession(session: RTCSession) {
        Timber.d("onReceiveNewSession")
        if (currentSession != null) {
            Timber.d("reject new session, device is busy")
            session.rejectCall(null)
        }
    }

    override fun onUserNoActions(session: RTCSession?, userId: Int?) {
    }

    override fun onSessionClosed(session: RTCSession) {
        Timber.d("onSessionClosed session= $session")
        if (session == currentSession) {
            Timber.d("release currentSession")
            releaseCurrentCall()
            ringtoneManager.stop()
            finish()
        }
    }

    override fun onCallRejectByUser(session: RTCSession?,
                                    userId: Int?,
                                    data: MutableMap<String, String>?
    ) {
    }

    private fun hangUpCurrentSession() {
        ringtoneManager.stop()
        currentSession?.hangUp(HashMap<String, String>())
    }

    private fun releaseCurrentCall() {
        audioManager?.stop()
        RTCClient.getInstance(this).removeSessionsCallbacksListener(this)
        currentSession?.removeSessionCallbacksListener(this)
        currentSession = null
        RTCSessionManager.getInstance().endCall()
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

    private inner class CameraSwitchHandler : CameraVideoCapturer.CameraSwitchHandler {
        override fun onCameraSwitchDone(isFront: Boolean) {
            toggle_camera.isEnabled = true
            callViewModel.callSessionAction.value = CallViewModel.CallSessionAction.SWITCHED_CAMERA
        }

        override fun onCameraSwitchError(err: String?) {
            Toast.makeText(
                applicationContext, getString(R.string.camera_switch_error),
                Toast.LENGTH_SHORT
            ).show()
            toggle_camera.isEnabled = true
        }
    }

    private inner class ConnectionListener : AbstractConnectionListener() {
        override fun connectionClosedOnError(e: Exception?) {
            showSnackbar(
                this@CallActivity,
                R.string.connection_is_disconnected,
                Snackbar.LENGTH_INDEFINITE
            )
        }

        override fun reconnectionSuccessful() {
            showSnackbar(
                this@CallActivity,
                R.string.connection_is_reconnected,
                Snackbar.LENGTH_SHORT
            )
        }

        override fun reconnectingIn(seconds: Int) {

        }
    }
}