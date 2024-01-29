package com.connectycube.messenger

import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.connectycube.messenger.helpers.EXTRA_CALL_TYPE
import com.connectycube.messenger.helpers.startCall
import com.connectycube.users.models.ConnectycubeUser
import java.util.*

const val MAX_OPPONENTS_LIMIT = 3

class SelectCallMembersActivity : SelectUsersFromExistActivity() {

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItemDone: MenuItem? = menu?.findItem(R.id.action_done)
        when(intent.getIntExtra(EXTRA_CALL_TYPE, -1)){
            1 -> menuItemDone?.icon = resources.getDrawable(R.drawable.ic_video_call_white_24dp)
            2 -> menuItemDone?.icon = resources.getDrawable(R.drawable.ic_phone_white_24dp)
        }
        menuItemDone?.isVisible = getSelectedUsers().isNotEmpty()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_done -> startCall()
        }

        return true
    }

    private fun startCall() {
        startCall(this, ArrayList(getSelectedUsers().map { it.id }), intent.getIntExtra(EXTRA_CALL_TYPE, -1))
        finish()
    }

    override fun onUserSelected(user: ConnectycubeUser, checked: Boolean) {
        if(checked && getSelectedUsers().size == MAX_OPPONENTS_LIMIT){
            Toast.makeText(this,
                getString(R.string.limit_users_to_selection,
                    MAX_OPPONENTS_LIMIT.toString()),
                Toast.LENGTH_LONG)
                .show()
            notifyUserAdapterDataSetChanged()
            return
        }

        super.onUserSelected(user, checked)
    }
}