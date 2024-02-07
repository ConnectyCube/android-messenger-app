package com.connectycube.messenger.helpers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.connectycube.messenger.EXTRA_USERS_TO_LOAD
import com.connectycube.messenger.SelectCallMembersActivity
import com.connectycube.ConnectyCube
import com.connectycube.webrtc.CallType

const val EXTRA_CALL_TYPE = "call_type"

const val CALL_TYPE_VIDEO = 1
const val CALL_TYPE_AUDIO = 2

fun startCall(context: Context, occupants: ArrayList<Int>, callType: Int) {
    val rtcCallType =
        if (callType == CALL_TYPE_VIDEO) CallType.VIDEO
        else CallType.AUDIO

    val p2pCalls = ConnectyCube.p2pCalls
    val rtcSession = p2pCalls.createSession(occupants, rtcCallType)

    RTCSessionManager.getInstance().startCall(rtcSession)

    Toast.makeText(context, "members = $occupants, callType = $callType", Toast.LENGTH_LONG).show()
}

fun startAudioCall(context: Context, allOccupants: ArrayList<Int>){
    if (allOccupants.size == 1){
        startCall(context, allOccupants, CALL_TYPE_AUDIO)
    } else {
        startOpponentsChoosing(context, allOccupants, CALL_TYPE_AUDIO)
    }
}

fun startVideoCall(context: Context, allOccupants: ArrayList<Int>){
    if (allOccupants.size == 1){
        startCall(context, allOccupants, CALL_TYPE_VIDEO)
    } else {
        startOpponentsChoosing(context, allOccupants, CALL_TYPE_VIDEO)
    }
}

private fun startOpponentsChoosing(context: Context, occupants: ArrayList<Int>, callType: Int) {
    val startIntent = Intent(context, SelectCallMembersActivity::class.java)
    startIntent.putIntegerArrayListExtra(EXTRA_USERS_TO_LOAD, occupants)
    startIntent.putExtra(EXTRA_CALL_TYPE, callType)
    context.startActivity(startIntent)
}