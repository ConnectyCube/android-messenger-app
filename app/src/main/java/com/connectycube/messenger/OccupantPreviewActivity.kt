package com.connectycube.messenger

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.connectycube.messenger.databinding.ActivityOccupantPreviewBinding
import com.connectycube.messenger.utilities.CREATED_AT_SIMPLE_DATE_FORMAT
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.utilities.getPrettyLastActivityDate
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.users.models.ConnectycubeUser
import java.text.SimpleDateFormat
import java.util.*

const val EXTRA_USER = "extra_user"

class OccupantPreviewActivity : BaseChatActivity() {
    private lateinit var binding: ActivityOccupantPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOccupantPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolBar()
        initUser()
    }

    private fun initToolBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initUser() {
        val user: ConnectycubeUser = intent.getSerializableExtra(EXTRA_USER) as ConnectycubeUser
        binding.userNameTxt.text = user.fullName ?: user.login
        loadUserAvatar(this, user, binding.avatarImg)

        val currentUser = SharedPreferencesManager.getInstance(this).getCurrentUser()
        val isCurrentUser = currentUser.id != null && user.id == currentUser.id
        if (!isCurrentUser) {
            binding.lastActivityTitleTxt.visibility = View.VISIBLE
            binding.lastActivityTextView.text =
                getPrettyLastActivityDate(this, SimpleDateFormat(
                    CREATED_AT_SIMPLE_DATE_FORMAT, Locale.getDefault()).parse(user.lastRequestAt))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_right)
    }
}