package com.connectycube.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.connectycube.ConnectyCube
import com.connectycube.messenger.databinding.FragmentAudioCallBinding
import com.connectycube.users.models.ConnectycubeUser

class AudioCallFragment :
    BaseCallFragment(R.string.title_audio_call) {
    val binding by lazy {
        _binding as FragmentAudioCallBinding
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAudioCallBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun initViews() {
        super.initViews()
        chronometerInCall = binding!!.chronometer
    }

    override fun textOpponentsNames(): TextView {
        return binding!!.outgoingView.textOpponentsNames
    }

    override fun layoutOutgoingView(): View {
        return binding!!.outgoingView.root
    }

    override fun buttonHangup(): View {
        return binding!!.callHangUp.buttonHangup
    }

    override fun initWithOpponents(opponents: List<ConnectycubeUser>?) {
        val opponentsFiltered =
            opponents?.filterNot { it.id == ConnectyCube.chat.userForLogin!!.id }
        val names = opponentsFiltered?.joinToString { it.fullName ?: it.login ?: "" }
        binding!!.textOtherName.text = names
    }
}