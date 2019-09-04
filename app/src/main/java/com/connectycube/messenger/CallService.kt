package com.connectycube.messenger

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import java.util.*

const val ACTION_SELECT_MEMBERS = "action_select_members"
const val ACTION_START_CALL = "action_start_call"

const val EXTRA_OCCUPANTS = "occupants"
const val EXTRA_CALL_TYPE = "call_type"

class CallService: Service() {


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent!!.action){
            ACTION_SELECT_MEMBERS -> handleSelectMembers(intent)
            ACTION_START_CALL -> handleStartCall(intent)

        }

        return START_NOT_STICKY
    }

    private fun handleStartCall(intent: Intent) {
        val members = intent.getIntegerArrayListExtra(EXTRA_OCCUPANTS)
        val callType = intent.getIntExtra(EXTRA_CALL_TYPE, -1)

        Toast.makeText(this, "members = $members, callType = $callType", Toast.LENGTH_LONG).show()
    }

    private fun handleSelectMembers(intent: Intent){
        val allOccupants = intent.getIntegerArrayListExtra(EXTRA_OCCUPANTS)
        if (allOccupants.size == 1){
            handleStartCall(intent)
        } else {
            startOpponentsChoosing(intent)
        }
    }

    private fun startOpponentsChoosing(intent: Intent) {

        val startIntent = Intent(this, SelectCallMembersActivity::class.java)
        startIntent.putIntegerArrayListExtra(EXTRA_OCCUPANTS, intent.getIntegerArrayListExtra(
            EXTRA_OCCUPANTS))
        startIntent.putExtra(EXTRA_CALL_TYPE, intent.getIntExtra(EXTRA_CALL_TYPE, -1))
        startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startIntent)
    }

    private fun startCall(allOccupants: ArrayList<Int>?, intExtra: Int) {

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}