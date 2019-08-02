package com.connectycube.messenger

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP
import androidx.appcompat.app.ActionBar.DISPLAY_SHOW_TITLE
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.adapters.CheckableUsersAdapter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.CreateChatDialogViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser

class CreateChatDialogActivity : BaseChatActivity(), CheckableUsersAdapter.CheckableUsersAdapterCallback {

    private lateinit var createChatDialogViewModel: CreateChatDialogViewModel
    private lateinit var usersAdapter: CheckableUsersAdapter
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var progressbar: ProgressBar
    private var selectedUsers: MutableList<ConnectycubeUser> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat)
        initToolbar()
        loadViews()
        initUserAdapter()
        initViews()
        loadData()
    }

    private fun initToolbar() {
        supportActionBar?.displayOptions = DISPLAY_SHOW_TITLE or DISPLAY_HOME_AS_UP
        setTitle(R.string.new_chat)
    }

    private fun loadViews() {
        usersRecyclerView = findViewById(R.id.list_users)
        progressbar = findViewById(R.id.progressbar)
    }

    private fun initUserAdapter() {
        usersAdapter = CheckableUsersAdapter(this, this)
    }

    private fun initViews() {
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        usersRecyclerView.itemAnimator = DefaultItemAnimator()
        usersRecyclerView.adapter = usersAdapter
    }

    private fun loadData() {
        createChatDialogViewModel = ViewModelProviders.of(
            this,
            InjectorUtils.provideCreateChatDialogViewModelFactory(this)
        ).get()

        createChatDialogViewModel.getLiveSelectedUsers().observe(this) { liveSelectedUsers ->
            selectedUsers = liveSelectedUsers
            invalidateOptionsMenu()
        }

        createChatDialogViewModel.getUsers(this).observe(this) { resource ->
            if (resource.status == Status.SUCCESS) {
                hideProgress(progressbar)

                val loadedUsers: List<ConnectycubeUser>? = resource.data
                loadedUsers?.let { usersAdapter.setItems(loadedUsers) }
            } else if (resource.status == Status.LOADING) {
                showProgress(progressbar)
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

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_done -> createChatDialog()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun createChatDialog() {
        createChatDialogViewModel.createNewChatDialog(this).observe(this) { resource ->
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
        val intent = Intent(this, ChatActivity::class.java)
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