package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.viewmodels.UserDetailsViewModel
import com.connectycube.messenger.vo.Status
import com.yalantis.ucrop.UCrop
import com.zhihu.matisse.Matisse
import kotlinx.android.synthetic.main.activity_settings.*
import com.connectycube.users.models.ConnectycubeUser
import timber.log.Timber

const val MAX_USER_NAME_LENGTH = 60
const val REQUEST_EDIT_USER_NAME = 100
const val EXTRA_LOGOUT = "chat_logout"

class SettingsActivity : BaseChatActivity() {
    private val permissionsHelper = PermissionsHelper(this)
    var currentUser: ConnectycubeUser? = null

    private val userDetailsViewModel: UserDetailsViewModel by viewModels {
        InjectorUtils.provideUserViewModelFactory(
            this.application,
            SharedPreferencesManager.getInstance(this).getCurrentUser().id
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
        edit_name_fb.setSingleOnClickListener { editName() }
        avatar_img.setSingleOnClickListener { editAvatar() }
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
                    }
                }
                Status.LOADING -> {
                    showProgressValueIfNotNull(progressbar, resource.progress)
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
        SharedPreferencesManager.getInstance(applicationContext).updateCurrentUserName(updatedUser)
        SharedPreferencesManager.getInstance(applicationContext)
            .updateCurrentUserAvatar(updatedUser)
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
        if (permissionsHelper.areAllImageGranted()) {
            requestImage(this)
        } else permissionsHelper.requestImagePermissions()
    }

    private fun startNameUpdate(newName: String?) {
        if (newName.isNullOrEmpty()) {
            Toast.makeText(this, R.string.username_can_not_be_empty, Toast.LENGTH_SHORT).show()
        } else if (currentUser?.fullName != newName) {
            Timber.d("startNameUpdate newName= $newName")
            userDetailsViewModel.updateName(newName)
        }
    }

    private fun startAvatarUpdate(path: String?) {
        path?.let {
            userDetailsViewModel.updateAvatar(path)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED || data == null) return

        when (requestCode) {
            REQUEST_EDIT_USER_NAME -> {
                startNameUpdate(data.getStringExtra(EXTRA_DATA))
            }
            REQUEST_CODE_CHOOSE -> {
                if (Matisse.obtainPathResult(data) != null) {
                    cropImage(this, Matisse.obtainPathResult(data).iterator().next())
                }
            }
            UCrop.REQUEST_CROP -> {
                val resultUri = UCrop.getOutput(data)
                resultUri?.let {
                    startAvatarUpdate(resultUri.path)
                }
            }
            UCrop.RESULT_ERROR -> {
                handleCropError(this, data)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_IMAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (permissionsHelper.areAllImageGranted()) {
                    Timber.d("permission was granted")
                } else {
                    Timber.d("permission is denied")
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
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

    private fun logout() {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_LOGOUT, true)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }
}