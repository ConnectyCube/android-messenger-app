package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.messenger.adapters.DialogOccupantsAdapter
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.viewmodels.ChatDialogDetailsViewModel
import com.connectycube.messenger.vo.Status
import com.yalantis.ucrop.UCrop
import com.zhihu.matisse.Matisse
import kotlinx.android.synthetic.main.activity_chat_dialog_details.*
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeDialogType
import com.connectycube.users.models.ConnectycubeUser
import timber.log.Timber

const val EXTRA_CHAT_DIALOG_ID = "chat_dialog_id"
const val MAX_DIALOG_DESCRIPTION_LENGTH = 200
const val MAX_DIALOG_NAME_LENGTH = 60
const val REQUEST_EDIT_DESCRIPTION = 8
const val REQUEST_EDIT_NAME = 9
const val REQUEST_ADD_OCCUPANTS = 10
const val REQUEST_REMOVE_OCCUPANTS = 11

class ChatDialogDetailsActivity : BaseChatActivity(),
    DialogOccupantsAdapter.DialogOccupantsAdapterCallback {

    private val permissionsHelper = PermissionsHelper(this)
    private lateinit var chatDialogDetailsViewModel: ChatDialogDetailsViewModel
    private lateinit var currentChatDialog: ConnectycubeDialog
    private lateinit var occupantsAdapter: DialogOccupantsAdapter
    private var dialogOccupants: MutableList<ConnectycubeUser> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_dialog_details)
        initToolbar()
        initUserAdapter()
        initViews()
        loadData()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        back_btn.setOnClickListener { onBackPressed() }
        edit_group_name_btn.setSingleOnClickListener { editGroupName() }
        edit_grop_description_btn.setSingleOnClickListener { editGroupDescription() }
        edit_avatar_btn.setSingleOnClickListener { editGroupPhoto() }
        add_occupants_img.setSingleOnClickListener { addOccupants() }
        remove_occupants_img.setSingleOnClickListener { removeOccupants() }
    }

    private fun editGroupPhoto() {
        if (permissionsHelper.areAllImageGranted()) {
            requestImage(this)
        } else permissionsHelper.requestImagePermissions()
    }

    private fun editGroupName() {
        currentChatDialog.run {
            val intent = Intent(this@ChatDialogDetailsActivity, EditTextActivity::class.java)
            intent.putExtra(EXTRA_TITLE, getText(R.string.edit_group_name))
            intent.putExtra(EXTRA_EXIST_VALUE, name)
            intent.putExtra(EXTRA_MAX_LENGTH, MAX_DIALOG_NAME_LENGTH)
            intent.putExtra(EXTRA_HINT, getText(R.string.group_name))
            intent.putExtra(EXTRA_DESCRIPTION, getText(R.string.put_name_for_group))

            startActivityForResult(intent, REQUEST_EDIT_NAME)
        }
    }

    private fun editGroupDescription() {
        currentChatDialog.let {
            val intent = Intent(this, EditTextActivity::class.java)
            intent.putExtra(EXTRA_TITLE, getText(R.string.edit_group_description))
            intent.putExtra(EXTRA_EXIST_VALUE, it.description)
            intent.putExtra(EXTRA_MAX_LENGTH, MAX_DIALOG_DESCRIPTION_LENGTH)
            intent.putExtra(EXTRA_HINT, getText(R.string.group_description))
            intent.putExtra(EXTRA_DESCRIPTION, getText(R.string.put_there_some_information_about_group))

            startActivityForResult(intent, REQUEST_EDIT_DESCRIPTION)
        }
    }

    private fun initUserAdapter() {
        occupantsAdapter = DialogOccupantsAdapter(this, this, ::onOccupantClicked)
    }

    private fun initViews() {
        occupants_recycler_view.layoutManager = LinearLayoutManager(this)
        occupants_recycler_view.itemAnimator = DefaultItemAnimator()
        occupants_recycler_view.adapter = occupantsAdapter
    }

    private fun loadData() {
        chatDialogDetailsViewModel = getViewModel(intent.getStringExtra(EXTRA_CHAT_DIALOG_ID)!!)

        chatDialogDetailsViewModel.liveDialog.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    hideProgress(progressbar)
                    resource.data?.let { chatDialog ->
                        attachData(chatDialog)
                    }
                }
                Status.LOADING -> {
                    showProgressValueIfNotNull(progressbar, resource.progress)
                }
                Status.ERROR -> {
                    hideProgress(progressbar)
                    resource.data?.let { chatDialog ->
                        attachData(chatDialog)
                    }
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun attachData(chatDialog: ConnectycubeDialog) {
        currentChatDialog = chatDialog

        if (currentChatDialog.type == ConnectycubeDialogType.PRIVATE) {
            group_description_layout.visibility = View.GONE
            add_occupants_img.visibility = View.GONE
            edit_group_name_btn.visibility = View.GONE
            if (!isUserCreator(getCurrentUser())) edit_avatar_btn.visibility = View.GONE
        } else if (!isUserCreator(getCurrentUser()) && !isUserAdmin(getCurrentUser())) {
            edit_grop_description_btn.visibility = View.GONE
            edit_avatar_btn.visibility = View.GONE
            edit_group_name_btn.visibility = View.GONE
        }

        chatDialogDetailsViewModel.getUsers(chatDialog).observe(this, Observer { resource ->
            when (resource.status) {
                Status.LOADING -> {
                    occupants_progress.visibility = View.VISIBLE
                }
                Status.ERROR -> {
                    occupants_progress.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                Status.SUCCESS -> {
                    occupants_progress.visibility = View.GONE
                    val occupants: List<ConnectycubeUser>? = resource.data
                    occupants?.let {
                        dialogOccupants = ArrayList(occupants)
                        occupantsAdapter.setItems(occupants)
                    }
                }
            }
        })

        description_txt.text = chatDialog.description
        chat_dialog_name_txt.text = chatDialog.name
        remove_occupants_img.visibility = if (isUserAdmin(getCurrentUser()) || isUserCreator(getCurrentUser())) View.VISIBLE else View.GONE
        loadChatDialogPhoto(this, chatDialog.type == ConnectycubeDialogType.PRIVATE, chatDialog.photo, avatar_img)
    }

    private fun addOccupants() {
        val intent = Intent(this, SelectUsersActivity::class.java)
        intent.putIntegerArrayListExtra(EXTRA_FILTER_IDS, ArrayList(currentChatDialog.occupantsIds))
        startActivityForResult(intent, REQUEST_ADD_OCCUPANTS)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun removeOccupants() {
        val intent = Intent(this, SelectUsersFromExistActivity::class.java)
        intent.putIntegerArrayListExtra(EXTRA_USERS_TO_LOAD, ArrayList(currentChatDialog.occupantsIds!!.filter { it != currentChatDialog.userId }))
        startActivityForResult(intent, REQUEST_REMOVE_OCCUPANTS)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun getViewModel(dialogId: String): ChatDialogDetailsViewModel {
        val dialogViewModel: ChatDialogDetailsViewModel by viewModels {
            InjectorUtils.provideChatDialogDetailsViewModelFactory(this.application, dialogId)
        }

        return dialogViewModel
    }

    private fun onOccupantClicked(user: ConnectycubeUser) {
        startOccupantPreview(user)
    }

    private fun startOccupantPreview(user: ConnectycubeUser) {
        val intent = Intent(this, OccupantPreviewActivity::class.java)
        intent.putExtra(EXTRA_USER, user)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED || data == null) return

        when (requestCode) {
            REQUEST_EDIT_DESCRIPTION -> {
                startDescriptionUpdate(data.getStringExtra(EXTRA_DATA))
            }
            REQUEST_EDIT_NAME -> {
                startNameUpdate(data.getStringExtra(EXTRA_DATA))
            }
            REQUEST_ADD_OCCUPANTS -> {
                startAddOccupants(data.getIntegerArrayListExtra(EXTRA_SELECTED_USERS)!!)
            }
            REQUEST_REMOVE_OCCUPANTS -> {
                startRemoveOccupants(data.getIntegerArrayListExtra(EXTRA_SELECTED_USERS)!!)
            }
//            update photo
            REQUEST_CODE_CHOOSE -> {
                if (Matisse.obtainPathResult(data) != null) {
                    cropImage(this, Matisse.obtainPathResult(data).iterator().next())
                }
            }
            UCrop.REQUEST_CROP -> {
                val resultUri = UCrop.getOutput(data)
                resultUri?.let {
                    startPhotoUpdate(resultUri.path)
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

    private fun startPhotoUpdate(path: String?) {
        path?.let {
            chatDialogDetailsViewModel.updateGroupPhoto(currentChatDialog.dialogId!!, path)
        }
    }

    private fun startDescriptionUpdate(newDescription: String?) {
        if (newDescription.isNullOrEmpty()) {
            Toast.makeText(this, R.string.group_description_can_not_be_empty, Toast.LENGTH_LONG).show()
        } else if (currentChatDialog.description != newDescription) {
            chatDialogDetailsViewModel.updateGroupDescription(currentChatDialog.dialogId!!, newDescription)
        }
    }

    private fun startNameUpdate(newName: String?) {
        if (newName.isNullOrEmpty()) {
            Toast.makeText(this, R.string.group_name_can_not_be_empty, Toast.LENGTH_LONG).show()
        } else if (currentChatDialog.description != newName) {
            chatDialogDetailsViewModel.updateGroupName(currentChatDialog.dialogId!!, newName)
        }
    }

    private fun startAddOccupants(selectedUsers: java.util.ArrayList<Int>) {
        chatDialogDetailsViewModel.addOccupants(
            currentChatDialog.dialogId!!,
            *selectedUsers.toIntArray()
        )
    }

    private fun startRemoveOccupants(selectedUsers: java.util.ArrayList<Int>) {
        chatDialogDetailsViewModel.removeOccupants(
            currentChatDialog.dialogId!!,
            *selectedUsers.toIntArray()
        )
    }

    override fun onAddUserToAdmins(userId: Int) {
        chatDialogDetailsViewModel.addUserToAdmins(currentChatDialog.dialogId!!, userId)
    }

    override fun onRemoveUserFromAdmins(userId: Int) {
        chatDialogDetailsViewModel.removeUserFromAdmins(currentChatDialog.dialogId!!, userId)
    }

    override fun onRemoveUserFromOccupants(userId: Int) {
        chatDialogDetailsViewModel.removeOccupants(currentChatDialog.dialogId!!, userId)
    }

    override fun isUserCreator(user: ConnectycubeUser): Boolean {
        return currentChatDialog.userId == user.id
    }

    override fun isUserAdmin(user: ConnectycubeUser): Boolean {
        return currentChatDialog.adminsIds!!.contains(user.id)
    }

    override fun isCurrentUser(user: ConnectycubeUser): Boolean {
        return chatDialogDetailsViewModel.isCurrentUser(user)
    }

    override fun getCurrentUser(): ConnectycubeUser {
        return chatDialogDetailsViewModel.getCurrentUser()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(REQUEST_CODE_DETAILS)
    }
}