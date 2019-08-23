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
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.messenger.adapters.DialogOccupantsAdapter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.loadChatDialogPhoto
import com.connectycube.messenger.viewmodels.ChatDialogDetailsViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_chat_dialog_details.*
import kotlinx.android.synthetic.main.activity_chat_dialog_details.add_occupants_img
import kotlinx.android.synthetic.main.activity_chat_dialog_details.avatar_img
import kotlinx.android.synthetic.main.activity_chat_dialog_details.chat_dialog_id
import kotlinx.android.synthetic.main.activity_chat_dialog_details.chat_dialog_name_txt
import kotlinx.android.synthetic.main.activity_chat_dialog_details.description_txt
import kotlinx.android.synthetic.main.activity_chat_dialog_details.edit_grop_description_btn
import kotlinx.android.synthetic.main.activity_chat_dialog_details.group_description_layout
import kotlinx.android.synthetic.main.activity_chat_dialog_details.occupants_progress
import kotlinx.android.synthetic.main.activity_chat_dialog_details.occupants_recycler_view
import kotlinx.android.synthetic.main.activity_chat_dialog_details.progressbar
import kotlinx.android.synthetic.main.activity_chat_dialog_details.toolbar

const val EXTRA_CHAT_DIALOG_ID = "chat_dialog_id"
const val MAX_DIALOG_DESCRIPTION_LENGTH = 200
const val MAX_DIALOG_NAME_LENGTH = 60
const val REQUEST_EDIT_DESCRIPTION = 8
const val REQUEST_EDIT_NAME = 9

class ChatDialogDetailsActivity : BaseChatActivity(),
    DialogOccupantsAdapter.DialogOccupantsAdapterCallback {

    private lateinit var chatDialogDetailsViewModel: ChatDialogDetailsViewModel
    private lateinit var currentChatDialog: ConnectycubeChatDialog
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
        open_chat_dialog_btn.setOnClickListener { startChatDialog() }
        edit_group_name_btn.setOnClickListener { editGroupName() }
        edit_grop_description_btn.setOnClickListener { editGroupDescription() }
        edit_avatar_btn.setOnClickListener { editGroupPhoto() }
        add_occupants_img.setOnClickListener { addOccupants() }

    }

    private fun editGroupPhoto() {
        Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_LONG).show()
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

    private fun startChatDialog() {
        currentChatDialog.let {
            val intent = Intent(this, ChatMessageActivity::class.java)
            intent.putExtra(EXTRA_CHAT, it)
            startActivity(intent)
        }
    }

    private fun initUserAdapter() {
        occupantsAdapter = DialogOccupantsAdapter(this, this)
    }

    private fun initViews() {
        occupants_recycler_view.layoutManager = LinearLayoutManager(this)
        occupants_recycler_view.itemAnimator = DefaultItemAnimator()
        occupants_recycler_view.adapter = occupantsAdapter
    }

    private fun loadData() {
        chatDialogDetailsViewModel = getViewModel(intent.getStringExtra(EXTRA_CHAT_DIALOG_ID))

        chatDialogDetailsViewModel.liveDialog.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    hideProgress(progressbar)
                    resource.data?.let { chatDialog ->
                        attachData(chatDialog)
                    }
                }
                Status.LOADING -> {
                    showProgress(progressbar)
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

    private fun attachData(chatDialog: ConnectycubeChatDialog) {
        currentChatDialog = chatDialog
        if (currentChatDialog.isPrivate) edit_group_name_btn.visibility = View.INVISIBLE
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

        group_description_layout.visibility =
            if (chatDialog.type == ConnectycubeDialogType.PRIVATE) View.GONE else View.VISIBLE
        description_txt.text = chatDialog.description
        add_occupants_img.visibility =
            if (chatDialog.type == ConnectycubeDialogType.PRIVATE) View.GONE else View.VISIBLE
//        remove_occupants_img.visibility =
//            if (chatDialog.type == ConnectycubeDialogType.GROUP && isCurrentUserCreator()) View.VISIBLE else View.GONE


        edit_avatar_btn.visibility = if (chatDialog.type == ConnectycubeDialogType.PRIVATE) View.GONE else View.VISIBLE

        chat_dialog_name_txt.text = chatDialog.name
        chat_dialog_id.text = getString(R.string.id_format, chatDialog.dialogId)
        loadChatDialogPhoto(this, chatDialog.isPrivate, chatDialog.photo, avatar_img)
    }

    private fun addOccupants() {
        Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_LONG).show()
    }

    private fun getViewModel(dialogId: String): ChatDialogDetailsViewModel {
        val chatMessageListViewModel: ChatDialogDetailsViewModel by viewModels {
            InjectorUtils.provideChatDialogDetailsViewModelFactory(this.application, dialogId)
        }

        return chatMessageListViewModel
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
        }
    }

    private fun startDescriptionUpdate(newDescription: String?) {
        if (newDescription.isNullOrEmpty()) {
            Toast.makeText(this, R.string.group_description_can_not_be_empty, Toast.LENGTH_LONG).show()
        } else if (currentChatDialog.description != newDescription) {
            chatDialogDetailsViewModel.updateGroupDescription(currentChatDialog.dialogId, newDescription)
        }
    }

    private fun startNameUpdate(newName: String?) {
        if (newName.isNullOrEmpty()) {
            Toast.makeText(this, R.string.group_name_can_not_be_empty, Toast.LENGTH_LONG).show()
        } else if (currentChatDialog.description != newName) {
            chatDialogDetailsViewModel.updateGroupName(currentChatDialog.dialogId, newName)
        }
    }

    override fun onAddUserToAdmins(userId: Int) {
        chatDialogDetailsViewModel.addUserToAdmins(currentChatDialog.dialogId, userId)
    }

    override fun onRemoveUserFromAdmins(userId: Int) {
        chatDialogDetailsViewModel.removeUserFromAdmins(currentChatDialog.dialogId, userId)
    }

    override fun onRemoveUserFromOccupants(userId: Int) {
        chatDialogDetailsViewModel.removeOccupantUser(currentChatDialog.dialogId, userId)
    }

    override fun isUserCreator(user: ConnectycubeUser): Boolean {
        return currentChatDialog.userId == user.id
    }

    override fun isUserAdmin(user: ConnectycubeUser): Boolean {
        return currentChatDialog.adminsIds.contains(user.id)
    }

    override fun isCurrentUser(user: ConnectycubeUser): Boolean {
        return chatDialogDetailsViewModel.isCurrentUser(user)
    }

    override fun getCurrentUser(): ConnectycubeUser {
        return chatDialogDetailsViewModel.getCurrentUser()
    }

    private fun isCurrentUserCreator(): Boolean {
        return isUserCreator(chatDialogDetailsViewModel.getCurrentUser())
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }
}