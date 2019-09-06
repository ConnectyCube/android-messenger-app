package com.connectycube.messenger

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.messenger.viewmodels.CallViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import com.connectycube.videochat.RTCSession
import com.connectycube.videochat.RTCTypes
import kotlinx.android.synthetic.main.fragment_incoming_call.*


class IncomingCallFragment : Fragment(R.layout.fragment_incoming_call) {
    private var currentSession: RTCSession? = null
    private var opponentsIds: List<Int>? = null
    private var conferenceType: RTCTypes.ConferenceType? = null

    private lateinit var callViewModel: CallViewModel

    enum class CallAction {
        ACCEPT, REJECT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        retainInstance = true
        super.onCreate(savedInstanceState)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setTitle(R.string.title_incoming_call)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.let {
            currentSession = RTCSessionManager.getInstance(it.applicationContext).currentCall
            callViewModel = ViewModelProviders.of(it).get(CallViewModel::class.java)
        }
        initArguments()
        initFields()
    }

    private fun initArguments() {
        currentSession?.let {
            opponentsIds = it.opponents
            conferenceType = it.conferenceType
        }
    }

    private fun initFields() {
        currentSession?.let { session ->
            val ids = ArrayList<Int>(session.opponents.apply { add(session.callerID) }).toIntArray()
            callViewModel.getOpponents(*ids).observe(this, Observer { result ->
                if (result.status == Status.SUCCESS) {
                    val callerUser: ConnectycubeUser =
                        result.data!!.first { it.id == session.callerID }
                    loadUserAvatar(context!!, callerUser, image_avatar)
                    text_name.text = callerUser.fullName ?: callerUser.login
                    val opponents =
                        result.data.filterNot { it.id != session.callerID || it.id != ConnectycubeChatService.getInstance().user.id }
                    val names =
                        opponents.map { it.fullName ?: it.login }.joinToString()
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
        val isVideoCall = conferenceType == RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO
        text_call_type.text =
            if (isVideoCall) getString(R.string.incoming_video_call_title) else getString(R.string.incoming_audio_call_title)
    }

    private fun initButtons() {
        button_reject_call.setOnClickListener { reject() }
        button_accept_call.setOnClickListener { accept() }
    }

    private fun reject() {
        callViewModel.incomingCallAction.value = CallAction.REJECT
    }

    private fun accept() {
        callViewModel.incomingCallAction.value = CallAction.ACCEPT
    }

}