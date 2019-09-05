package com.connectycube.messenger

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.messenger.adapters.CheckableUsersAdapter
import com.connectycube.messenger.helpers.EXTRA_CALL_TYPE
import com.connectycube.messenger.helpers.EXTRA_OCCUPANTS
import com.connectycube.messenger.helpers.startCall
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.SelectCallMembersViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_create_chat.progressbar
import kotlinx.android.synthetic.main.activity_create_chat.users_recycler_view
import kotlinx.android.synthetic.main.activity_select_users.*
import java.util.*

class SelectCallMembersActivity : BaseChatActivity(), CheckableUsersAdapter.CheckableUsersAdapterCallback {

    private val selectCallMembersViewModel: SelectCallMembersViewModel by viewModels {
        InjectorUtils.provideSelectCallMembersViewModelFactory(this.application)
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
        selectCallMembersViewModel.liveSelectedUsers.observe(this) { liveSelectedUsers ->
            selectedUsers = liveSelectedUsers
            invalidateOptionsMenu()
        }
        selectCallMembersViewModel.updateSelectedUsersStates()

        selectCallMembersViewModel.getUsers(intent.getIntegerArrayListExtra(EXTRA_OCCUPANTS)).observe(this) { result ->
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
        when(intent.getIntExtra(EXTRA_CALL_TYPE, -1)){
            1 -> menuItemDone?.icon = resources.getDrawable(R.drawable.ic_video_call_white_24dp)
            2 -> menuItemDone?.icon = resources.getDrawable(R.drawable.ic_phone_white_24dp)
        }
        menuItemDone?.isVisible = selectedUsers.isNotEmpty()

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_done -> startCall()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startCall() {
        startCall(this, ArrayList(selectedUsers.map { it.id }), intent.getIntExtra(EXTRA_CALL_TYPE, -1))
        finish()
    }

    override fun onUserSelected(user: ConnectycubeUser, checked: Boolean) {
        selectCallMembersViewModel.updateUserSelection(user, checked)
    }

    override fun isUserSelected(user: ConnectycubeUser): Boolean {
        return selectedUsers.contains(user)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_right)
    }
}