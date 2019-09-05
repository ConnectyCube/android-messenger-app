package com.connectycube.messenger

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.connectycube.messenger.viewmodels.CallViewModel
import com.connectycube.videochat.RTCSession

abstract class BaseCallFragment(
    @LayoutRes contentLayoutId: Int
) : Fragment(contentLayoutId) {

    lateinit var callViewModel: CallViewModel
    var currentSession: RTCSession? = null
    var isIncomingCall: Boolean = false

    companion object {
        fun createInstance(fragment: BaseCallFragment, isIncomingCall: Boolean): BaseCallFragment {
            val args = Bundle()
            args.putBoolean(EXTRA_IS_INCOMING_CALL, isIncomingCall)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        currentSession = RTCSessionManager.getInstance().getCurrentSession()
        arguments?.let{
            isIncomingCall = it.getBoolean(EXTRA_IS_INCOMING_CALL)
        }
        activity?.let {
            callViewModel = ViewModelProviders.of(it).get(CallViewModel::class.java)
        }

    }
}