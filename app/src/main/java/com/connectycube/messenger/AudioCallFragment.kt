package com.connectycube.messenger

import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.fragment_audio_call.*

class AudioCallFragment :
    BaseCallFragment(R.layout.fragment_audio_call, R.string.title_audio_call) {

    override fun initViews() {
        super.initViews()
        chronometerInCall = chronometer
    }

    override fun initWithOpponents(opponents: List<ConnectycubeUser>?) {
        val opponentsFiltered =
            opponents?.filterNot { it.id == ConnectycubeChatService.getInstance().user.id }
        val names = opponentsFiltered?.joinToString { it.fullName ?: it.login }
        text_other_name.text = names
    }
}