package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP
import androidx.appcompat.app.ActionBar.DISPLAY_SHOW_TITLE
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.adapters.CheckableUsersAdapter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.CreateChatDialogViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_create_chat.*
import timber.log.Timber

class CreateChatDialogActivity : BaseChatActivity(),
    CheckableUsersAdapter.CheckableUsersAdapterCallback {

    private val createChatDialogViewModel: CreateChatDialogViewModel by viewModels {
        InjectorUtils.provideCreateChatDialogViewModelFactory(this.application)
    }

    private lateinit var usersAdapter: CheckableUsersAdapter
    private var selectedUsers: MutableList<ConnectycubeUser> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat)
        initToolbar()
        initUserAdapter()
        initViews()
        loadData()
    }

    private fun initToolbar() {
        supportActionBar?.displayOptions = DISPLAY_SHOW_TITLE or DISPLAY_HOME_AS_UP
        title = getText(R.string.new_chat)
    }

    private fun initUserAdapter() {
        usersAdapter = CheckableUsersAdapter(this, this)
    }

    private fun initViews() {
        users_recycler_view.layoutManager = LinearLayoutManager(this)
        users_recycler_view.itemAnimator = DefaultItemAnimator()
        users_recycler_view.adapter = usersAdapter
    }

    private fun loadData() {
        createChatDialogViewModel.liveSelectedUsers.observe(this) { liveSelectedUsers ->
            selectedUsers = liveSelectedUsers
            invalidateOptionsMenu()
        }
        createChatDialogViewModel.updateSelectedUsersStates()

        createChatDialogViewModel.getUsers().observe(this) { result ->
            when (result.status) {
                Status.LOADING -> showProgress(progressbar)
                Status.ERROR -> hideProgress(progressbar)
                Status.SUCCESS -> {
                    hideProgress(progressbar)
                    val users = result.data
                    if (users?.isNotEmpty()!!) {
                        usersAdapter.setItems(users)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.create_chat_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItemDone: MenuItem? = menu?.findItem(R.id.action_done)
        menuItemDone?.isVisible = selectedUsers.isNotEmpty()

        if (selectedUsers.size == 1) {
            menuItemDone?.icon = resources.getDrawable(R.drawable.ic_account_check)
        } else if (selectedUsers.size > 1){
            menuItemDone?.icon = resources.getDrawable(R.drawable.ic_account_multiple_check)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_done -> {
                val isPrivate = selectedUsers.size < 2
                if (!isPrivate) startCreateChatDialogDetailActivity()
                else createChatDialog()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startCreateChatDialogDetailActivity() {
        val intent = Intent(this, CreateChatDialogDetailActivity::class.java)
        startActivityForResult(intent, REQUEST_CREATE_DIALOG_DETAILS)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED || data == null) return
        when (requestCode) {
            REQUEST_CREATE_DIALOG_DETAILS -> {
                val name = data.getStringExtra(EXTRA_DIALOG_NAME)
                val avatar = data.getStringExtra(EXTRA_DIALOG_AVATAR)
                createChatDialog(name, avatar)
            }
        }
    }

    private fun createChatDialog(name: String? = null, avatar: String? = null) {
        Timber.d("name= $name, avatar= $avatar")
        createChatDialogViewModel.createNewChatDialog(name, avatar).observe(this) { resource ->
            when {
                resource.status == Status.SUCCESS -> {
                    hideProgress(progressbar)

                    val newChatDialog: ConnectycubeChatDialog? = resource.data
                    if (newChatDialog != null) {
                        startChatActivity(newChatDialog)
                        finish()
                    }
                }
                resource.status == Status.LOADING -> showProgress(progressbar)
                resource.status == Status.ERROR -> {
                    hideProgress(progressbar)
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startChatActivity(chat: ConnectycubeChatDialog) {
        val intent = Intent(this, ChatMessageActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_FORWARD_RESULT)
        intent.putExtra(EXTRA_CHAT, chat)
        startActivity(intent)
    }

    override fun onUserSelected(user: ConnectycubeUser, checked: Boolean) {
        createChatDialogViewModel.updateUserSelection(user, checked)
    }

    override fun isUserSelected(user: ConnectycubeUser): Boolean {
        return selectedUsers.contains(user)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_right)
    }
}