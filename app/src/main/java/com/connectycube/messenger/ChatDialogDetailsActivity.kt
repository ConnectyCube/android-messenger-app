package com.connectycube.messenger

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP
import androidx.appcompat.app.ActionBar.DISPLAY_SHOW_TITLE
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.messenger.adapters.DialogOccupantsAdapter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.ChatDialogDetailsViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_chat_dialog_details.*

const val EXTRA_CHAT_DIALOG_ID = "chat_dialog_id"

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
        supportActionBar?.displayOptions = DISPLAY_SHOW_TITLE or DISPLAY_HOME_AS_UP

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
                        currentChatDialog = chatDialog
                        chatDialogDetailsViewModel.getUsers(chatDialog).observe(this, Observer { resource ->
                            when (resource.status) {
                                Status.LOADING -> {
                                    occupants_progress.visibility = View.VISIBLE
                                }
                                Status.ERROR -> {
                                    occupants_progress.visibility = View.GONE
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

                        collapsing_toolbar.title = chatDialog.name
                        group_description_layout.visibility =
                            if (chatDialog.type == ConnectycubeDialogType.PRIVATE) View.GONE else View.VISIBLE
                        description_txt.text = chatDialog.description
                        add_occupants_img.visibility =
                            if (chatDialog.type == ConnectycubeDialogType.PRIVATE) View.GONE else View.VISIBLE
                        remove_occupants_img.visibility =
                            if (chatDialog.type == ConnectycubeDialogType.GROUP && isCurrentUserCreator()) View.VISIBLE else View.GONE
                    }
                }
                Status.LOADING -> {
                    showProgress(progressbar)
                }
                Status.ERROR -> {
                    hideProgress(progressbar)
                }
            }
        })
    }

    private fun getViewModel(dialogId: String): ChatDialogDetailsViewModel {
        val chatMessageListViewModel: ChatDialogDetailsViewModel by viewModels {
            InjectorUtils.provideChatDialogDetailsViewModelFactory(this.application, dialogId)
        }

        return chatMessageListViewModel
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.create_chat_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

//    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
//        val menuItemDone: MenuItem? = menu?.findItem(R.id.action_done)
//        menuItemDone?.isVisible = dialogOccupants.isNotEmpty()
//
//        return super.onPrepareOptionsMenu(menu)
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
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

    private fun isCurrentUserCreator(): Boolean {
        return isUserCreator(chatDialogDetailsViewModel.getCurrentUser())
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_right)
    }
}