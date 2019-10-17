package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.messenger.adapters.CheckableUsersAdapter
import com.connectycube.messenger.viewmodels.SelectUsersViewModel
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_create_chat.users_recycler_view
import kotlinx.android.synthetic.main.activity_select_users.*
import java.util.*

const val EXTRA_SELECTED_USERS = "selected_users"

abstract class SelectUsersBaseActivity<VM : SelectUsersViewModel> : BaseChatActivity(),
    CheckableUsersAdapter.CheckableUsersAdapterCallback {

    abstract fun getViewMode(): VM

    private lateinit var usersAdapter: CheckableUsersAdapter
    private var selectedUsers: MutableList<ConnectycubeUser> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_users)
        initToolbar()
        initUserAdapter()
        initViews()
        loadData()
    }

    protected fun initToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected fun initUserAdapter() {
        usersAdapter = CheckableUsersAdapter(this, this)
    }

    protected fun initViews() {
        users_recycler_view.layoutManager = LinearLayoutManager(this)
        users_recycler_view.itemAnimator = DefaultItemAnimator()
        users_recycler_view.adapter = usersAdapter
    }

    protected open fun loadData() {
        getViewMode().liveSelectedUsers.observe(this) { liveSelectedUsers ->
            selectedUsers = liveSelectedUsers
            invalidateOptionsMenu()
        }
        getViewMode().updateSelectedUsersStates()
    }

    protected open fun setUsers(users: List<ConnectycubeUser>?) {
        if (users?.isNotEmpty()!!) {
            usersAdapter.setItems(users)
            txt_empty_list.visibility = View.GONE
        } else {
            txt_empty_list.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.select_users_activity, menu)
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
            R.id.action_done -> returnSelectedUsers()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun returnSelectedUsers() {
        val result = Intent()
        result.putIntegerArrayListExtra(
            EXTRA_SELECTED_USERS,
            ArrayList(selectedUsers.map { it.id })
        )
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun onUserSelected(user: ConnectycubeUser, checked: Boolean) {
        getViewMode().updateUserSelection(user, checked)
    }

    override fun isUserSelected(user: ConnectycubeUser): Boolean {
        return selectedUsers.contains(user)
    }

    protected fun getSelectedUsers(): List<ConnectycubeUser> {
        return selectedUsers
    }

    protected fun notifyUserAdapterDataSetChanged() {
        usersAdapter.notifyDataSetChanged()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_right)
    }
}