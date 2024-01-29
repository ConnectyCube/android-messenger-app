package com.connectycube.messenger

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.helpers.RingtoneManager
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.messenger.viewmodels.CallViewModel
import com.connectycube.messenger.vo.Status
import kotlinx.android.synthetic.main.fragment_incoming_call.*
import com.connectycube.ConnectyCube
import com.connectycube.users.models.ConnectycubeUser
import com.connectycube.webrtc.CallType
import com.connectycube.webrtc.P2PSession
import timber.log.Timber


class IncomingCallFragment : Fragment(R.layout.fragment_incoming_call) {
    private var currentSession: P2PSession? = null
    private lateinit var ringtoneManager: RingtoneManager
    private var opponentsIds: List<Int>? = null
    private var conferenceType: CallType? = null

    private lateinit var callViewModel: CallViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        retainInstance = true
        super.onCreate(savedInstanceState)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setTitle(R.string.title_incoming_call)
        ringtoneManager = RingtoneManager(context!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.let {
            currentSession = RTCSessionManager.getInstance().currentCall
            callViewModel = ViewModelProviders.of(it).get(CallViewModel::class.java)
        }
        initArguments()
        initFields()
    }

    override fun onResume() {
        super.onResume()
        startRingtone()
    }

    override fun onPause() {
        super.onPause()
        stopRingtone()
    }

    private fun startRingtone() {
        Timber.d("startRingtone")
        ringtoneManager.start(looping = false, vibrate = true)
    }

    private fun stopRingtone() {
        Timber.d("stopRingtone()")
        ringtoneManager.stop()
    }

    private fun initArguments() {
        currentSession?.let {
            opponentsIds = it.getOpponents()
            conferenceType = it.getCallType()
        }
    }

    private fun initFields() {
        currentSession?.let { session ->
            val ids = ArrayList<Int>(session.getOpponents().toMutableList().apply { add(session.getCallerId())}).toIntArray()
            callViewModel.getOpponents(*ids).observe(this, Observer { result ->
                if (result.status == Status.SUCCESS) {
                    val callerUser: ConnectycubeUser =
                        result.data!!.first { it.id == session.getCallerId() }
                    loadUserAvatar(context!!, callerUser, image_avatar)
                    text_name.text = callerUser.fullName ?: callerUser.login
                    val opponentsFiltered =
                        result.data.filterNot { it.id != session.getCallerId() || it.id != ConnectyCube.chat.userForLogin!!.id }
                    val names = opponentsFiltered.joinToString { it.fullName ?: it.login?: "" }
                    if (names.isNotEmpty()) {
                        text_on_call.visibility = View.VISIBLE
                        text_other_name.text = names
                    }
                }
            })

            setCallType()
            initButtons()
        }
    }

    private fun setCallType() {
        val isVideoCall = conferenceType == CallType.VIDEO
        text_call_type.text =
            if (isVideoCall) getString(R.string.incoming_video_call_title) else getString(R.string.incoming_audio_call_title)
    }

    private fun initButtons() {
        button_reject_call.setOnClickListener { reject() }
        button_accept_call.setOnClickListener { accept() }
    }

    private fun reject() {
        callViewModel.incomingCallAction.value = CallViewModel.CallUserAction.REJECT
        stopRingtone()
    }

    private fun accept() {
        callViewModel.incomingCallAction.value = CallViewModel.CallUserAction.ACCEPT
        stopRingtone()
    }

}