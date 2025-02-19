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
import com.connectycube.messenger.databinding.ActivitySelectUsersBinding
import com.connectycube.messenger.viewmodels.SelectUsersViewModel
import com.connectycube.users.models.ConnectycubeUser
import java.util.*

const val EXTRA_SELECTED_USERS = "selected_users"

abstract class SelectUsersBaseActivity<VM : SelectUsersViewModel> : BaseChatActivity(),
    CheckableUsersAdapter.CheckableUsersAdapterCallback {

    abstract fun getViewMode(): VM

    protected lateinit var binding: ActivitySelectUsersBinding
    private lateinit var usersAdapter: CheckableUsersAdapter
    private var selectedUsers: MutableList<ConnectycubeUser> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.usersRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.usersRecyclerView.adapter = usersAdapter
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
            binding.txtEmptyList.visibility = View.GONE
        } else {
            binding.txtEmptyList.visibility = View.VISIBLE
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