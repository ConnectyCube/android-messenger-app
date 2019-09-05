package com.connectycube.messenger.helpers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.connectycube.messenger.SelectCallMembersActivity
import com.connectycube.videochat.RTCClient
import com.connectycube.videochat.RTCTypes

const val EXTRA_OCCUPANTS = "occupants"
const val EXTRA_CALL_TYPE = "call_type"

const val CALL_TYPE_VIDEO = 1
const val CALL_TYPE_AUDIO = 2

fun startCall(context: Context, occupants: ArrayList<Int>, callType: Int) {
    val rtcCallType =
        if (callType == CALL_TYPE_VIDEO) RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO
        else RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO

    val rtcClient = RTCClient.getInstance(context.applicationContext)
    val rtcSession = rtcClient.createNewSessionWithOpponents(occupants, rtcCallType)

    RTCSessionManager.getInstance(context).startCall(rtcSession)

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
    startIntent.putIntegerArrayListExtra(EXTRA_OCCUPANTS, occupants)
    startIntent.putExtra(EXTRA_CALL_TYPE, callType)
    context.startActivity(startIntent)
}