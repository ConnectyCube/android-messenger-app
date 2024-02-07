package com.connectycube.messenger

import kotlinx.android.synthetic.main.fragment_audio_call.*
import com.connectycube.ConnectyCube
import com.connectycube.users.models.ConnectycubeUser

class AudioCallFragment :
    BaseCallFragment(R.layout.fragment_audio_call, R.string.title_audio_call) {

    override fun initViews() {
        super.initViews()
        chronometerInCall = chronometer
    }

    override fun initWithOpponents(opponents: List<ConnectycubeUser>?) {
        val opponentsFiltered =
            opponents?.filterNot { it.id == ConnectyCube.chat.userForLogin!!.id }
        val names = opponentsFiltered?.joinToString { it.fullName ?: it.login?:"" }
        text_other_name.text = names
    }
}