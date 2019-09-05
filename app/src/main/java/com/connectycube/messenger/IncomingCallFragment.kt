package com.connectycube.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.connectycube.messenger.viewmodels.CallViewModel
import com.connectycube.videochat.RTCSession
import com.connectycube.videochat.RTCTypes
import androidx.lifecycle.ViewModelProviders
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.messenger.utilities.observeOnce
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
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
    }

//    override fun onCreateView(@NonNull inflater: LayoutInflater, container: ViewGroup?,
//                              savedInstanceState: Bundle?
//    ): View {
//        val view = inflater.inflate(R.layout.fragment_incoming_call, container, false)
//        initArguments()
////        ringtonePlayer = RingtonePlayer(activity)
//        return view
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initArguments()
        activity?.let {
            callViewModel = ViewModelProviders.of(it).get(CallViewModel::class.java)
        }
        initFields()
    }

    private fun initArguments() {
        //        currentSession = RTCManager.getInstance().getCurrentSession()
        currentSession?.let {
            opponentsIds = it.opponents
            conferenceType = it.conferenceType
        }
    }

    private fun initFields() {
        currentSession?.let { session ->
            callViewModel.getOpponents(session.callerID).observeOnce(this, Observer {
                if (it.status == Status.SUCCESS) {
                    val callerUser: ConnectycubeUser =
                        it.data!!.first { id == currentSession!!.callerID }
                    loadUserAvatar(context!!, callerUser, image_avatar)
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