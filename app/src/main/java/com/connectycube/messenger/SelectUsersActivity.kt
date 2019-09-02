package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.messenger.adapters.CheckableUsersAdapter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.SelectUsersViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_create_chat.progressbar
import kotlinx.android.synthetic.main.activity_create_chat.users_recycler_view
import kotlinx.android.synthetic.main.activity_select_users.*
import java.util.*

const val EXTRA_FILTER_IDS = "filter_ids"
const val EXTRA_SELECTED_USERS = "selected_users"

class SelectUsersActivity : BaseChatActivity(), CheckableUsersAdapter.CheckableUsersAdapterCallback {

    private val selectUsersViewModel: SelectUsersViewModel by viewModels {
        InjectorUtils.provideSelectUsersViewModelFactory(this.application)
    }

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

    private fun initToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
        selectUsersViewModel.liveSelectedUsers.observe(this) { liveSelectedUsers ->
            selectedUsers = liveSelectedUsers
            invalidateOptionsMenu()
        }
        selectUsersViewModel.updateSelectedUsersStates()

        selectUsersViewModel.getUsers(intent.extras.getIntegerArrayList(EXTRA_FILTER_IDS)).observe(this) { result ->
            when (result.status) {
                Status.LOADING -> showProgress(progressbar)
                Status.ERROR -> hideProgress(progressbar)
                Status.SUCCESS -> {
                    hideProgress(progressbar)
                    val users = result.data
                    if (users?.isNotEmpty()!!) {
                        usersAdapter.setItems(users)
                        txt_empty_list.visibility = View.GONE
                    } else {
                        txt_empty_list.visibility = View.VISIBLE
                    }
                }
            }
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
        result.putIntegerArrayListExtra(EXTRA_SELECTED_USERS, ArrayList(selectedUsers.map { it.id }))
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun onUserSelected(user: ConnectycubeUser, checked: Boolean) {
        selectUsersViewModel.updateUserSelection(user, checked)
    }

    override fun isUserSelected(user: ConnectycubeUser): Boolean {
        return selectedUsers.contains(user)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_right)
    }
}