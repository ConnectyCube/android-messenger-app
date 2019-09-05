package com.connectycube.messenger

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.messenger.utilities.observeOnce
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.fragment_audio_call.*
import kotlinx.android.synthetic.main.outgoing_view.*

class AudioCallFragment : BaseCallFragment(R.layout.fragment_audio_call) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOpponents()
        initViews()
        initButtons()
    }

    private fun initViews() {
        if (isIncomingCall) {
            layout_outgoing_view.visibility = View.GONE
        }
    }

    private fun initOpponents() {
        currentSession?.let { session ->
            callViewModel.getOpponents(session.callerID).observeOnce(this, Observer { resource ->
                if (resource.status == Status.SUCCESS) {
                    text_opponents_names.text =
                        resource.data?.map { it.fullName ?: it.login }.toString()
                }
            })
        }
    }

    private fun initButtons() {

    }
}