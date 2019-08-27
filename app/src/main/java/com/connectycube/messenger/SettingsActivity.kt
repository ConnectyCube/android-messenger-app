package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.messenger.api.UserService
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.messenger.viewmodels.UserDetailsViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

const val MAX_USER_NAME_LENGTH = 60
const val REQUEST_EDIT_USER_NAME = 100

class SettingsActivity : BaseChatActivity() {
    var currentUser: ConnectycubeUser? = null
    private val userDetailsViewModel: UserDetailsViewModel by viewModels {
        InjectorUtils.provideUserViewModelFactory(
            this.application,
            ConnectycubeChatService.getInstance().user.id
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initToolBar()
        initData()
        initViews()
    }

    private fun initToolBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initViews() {
        edit_name_fb.setOnClickListener { editName() }
        avatar_img.setOnClickListener { editAvatar() }
    }

    private fun initData() {
        userDetailsViewModel.liveDataUser.observe(this) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    hideProgress(progressbar)
                    resource.data?.let {
                        updateUserData(resource.data)
                        Timber.d("currentUser= $currentUser")
                        loadUserAvatar(this, currentUser!!, avatar_img)
                        user_name_txt.text = currentUser?.fullName ?: currentUser?.login
                        user_id.text = currentUser?.id.toString()
                    }
                }
                Status.LOADING -> {
                    showProgress(progressbar)
                }
                Status.ERROR -> {
                    hideProgress(progressbar)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateUserData(updatedUser: ConnectycubeUser) {
        currentUser = updatedUser
        SharedPreferencesManager.getInstance(applicationContext).updateUserName(updatedUser)
    }

    private fun editName() {
        currentUser?.let {
            val intent = Intent(this, EditTextActivity::class.java)
            intent.putExtra(EXTRA_TITLE, getText(R.string.edit_username))
            intent.putExtra(EXTRA_EXIST_VALUE, it.fullName)
            intent.putExtra(EXTRA_MAX_LENGTH, MAX_USER_NAME_LENGTH)
            intent.putExtra(EXTRA_HINT, getText(R.string.user_name_hint))
            intent.putExtra(EXTRA_DESCRIPTION, getText(R.string.username_description))

            startActivityForResult(intent, REQUEST_EDIT_USER_NAME)
        }
    }

    private fun editAvatar() {

    }

    private fun logout() {
        showProgress(progressbar)
        GlobalScope.launch(Dispatchers.Main) {
            UserService.instance.ultimateLogout(applicationContext)
            SharedPreferencesManager.getInstance(applicationContext).deleteCurrentUser()
            startLoginActivity()
            hideProgress(progressbar)

            finish()
        }
    }

    private fun startNameUpdate(newName: String?) {
        if (newName.isNullOrEmpty()) {
            Toast.makeText(this, R.string.username_can_not_be_empty, Toast.LENGTH_SHORT).show()
        } else if (currentUser?.fullName != newName) {
            Timber.d("startNameUpdate newName= $newName")
            userDetailsViewModel.updateName(newName)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED || data == null) return

        when (requestCode) {
            REQUEST_EDIT_USER_NAME -> {
                startNameUpdate(data.getStringExtra(EXTRA_DATA))
            }
        }
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this.startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                logout()
                item.isEnabled = false
                invalidateOptionsMenu()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                return true;
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }
}