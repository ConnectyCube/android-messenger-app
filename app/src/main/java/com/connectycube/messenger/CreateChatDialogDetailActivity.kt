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
import com.connectycube.messenger.viewmodels.CreateDialogDetailsViewModel
import com.yalantis.ucrop.UCrop
import com.zhihu.matisse.Matisse
import kotlinx.android.synthetic.main.activity_create_chat_details.*
import kotlinx.android.synthetic.main.activity_create_chat_details.avatar_img
import timber.log.Timber

const val REQUEST_CREATE_DIALOG_DETAILS = 250
const val EXTRA_DIALOG_NAME = "dialog_name"
const val EXTRA_DIALOG_AVATAR = "dialog_avatar"

class CreateChatDialogDetailActivity : BaseChatActivity() {
    private val permissionsHelper = PermissionsHelper(this)

    private val modelCreateDialogDetails: CreateDialogDetailsViewModel by viewModels {
        InjectorUtils.provideCreateDialogDetailsViewModelFactory(this.application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat_details)
        initToolbar()
        subscribeUi()
        edit_avatar_btn.setSingleOnClickListener { editAvatar() }
    }

    private fun initToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun editAvatar() {
        if (permissionsHelper.areAllImageGranted()) {
            requestImage(this)
        } else permissionsHelper.requestImagePermissions()
    }

    private fun subscribeUi() {
        modelCreateDialogDetails.liveDataResult.observe(this) { resource ->
            when {
                resource.status == com.connectycube.messenger.vo.Status.LOADING -> {
                    showProgress(progressbar)
                    progressbar.progress = resource.progress ?: 0
                }

                resource.status == com.connectycube.messenger.vo.Status.SUCCESS -> {
                    hideProgress(progressbar)
                    val url: String = resource.data!!
                    Timber.d(" startAvatarUpload url= $url")
                    loadChatDialogPhoto(this, false, url, avatar_img)
                }
                resource.status == com.connectycube.messenger.vo.Status.ERROR -> {
                    hideProgress(progressbar)
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.load_avatar_error, resource.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED || data == null) return

        when (requestCode) {
            REQUEST_CODE_CHOOSE -> {
                if (Matisse.obtainPathResult(data) != null) {
                    cropImage(this, Matisse.obtainPathResult(data).iterator().next())
                }
            }
            UCrop.REQUEST_CROP -> {
                val resultUri = UCrop.getOutput(data)
                resultUri?.let {
                    if (resultUri.path != null) startAvatarUpload(resultUri.path!!)
                }
            }
            UCrop.RESULT_ERROR -> {
                handleCropError(this, data)
            }
        }
    }

    private fun startAvatarUpload(path: String) {
        Timber.d("startAvatarUpload path=$path")
        modelCreateDialogDetails.uploadAvatar(path)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.create_chat_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> cancelResult()
            R.id.action_done -> setResult()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun cancelResult() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun setResult() {
        val name = chat_dialog_name_txt.text.toString()
        val avatar = modelCreateDialogDetails.photoUrl
        Timber.d("setResult avatar=$avatar, name= $name")
        if (name.isEmpty()) {
            Toast.makeText(
                applicationContext,
                getString(R.string.group_name_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_DIALOG_NAME, name)
        resultIntent.putExtra(EXTRA_DIALOG_AVATAR, avatar)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}