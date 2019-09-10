package com.connectycube.messenger

import android.os.Bundle
import android.view.View
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.messenger.viewmodels.CallViewModel
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.fragment_audio_call.*
import kotlinx.android.synthetic.main.outgoing_view.*

class AudioCallFragment :
    BaseCallFragment(R.layout.fragment_audio_call, R.string.title_audio_call) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun initViews() {
        inCallChronometer = chronometer
        super.initViews()
    }

    override fun initWithOpponents(opponents: List<ConnectycubeUser>?) {
        val opponentsFiltered =
            opponents?.filterNot { it.id == ConnectycubeChatService.getInstance().user.id }
        val names = opponentsFiltered?.joinToString { it.fullName ?: it.login }
        text_other_name.text = names
    }
}