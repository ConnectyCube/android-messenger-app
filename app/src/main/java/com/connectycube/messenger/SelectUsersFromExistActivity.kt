package com.connectycube.messenger

import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.SelectFromExistUsersViewModel
import com.connectycube.messenger.vo.Status

const val EXTRA_USERS_TO_LOAD = "users_to_load"

open class SelectUsersFromExistActivity : SelectUsersBaseActivity<SelectFromExistUsersViewModel>() {

    override fun getViewMode(): SelectFromExistUsersViewModel {
        val selectedUsersViewModel: SelectFromExistUsersViewModel by viewModels {
            InjectorUtils.provideSelectFromExistUsersViewModelFactory(this.application)
        }
        return selectedUsersViewModel
    }

    override fun loadData() {
        super.loadData()

        getViewMode().getUsers(intent.extras!!.getIntegerArrayList(EXTRA_USERS_TO_LOAD)!!)
            .observe(this) { result ->
                when (result.status) {
                    Status.LOADING -> showProgress(binding.progressbar)
                    Status.ERROR -> hideProgress(binding.progressbar)
                    Status.SUCCESS -> {
                        hideProgress(binding.progressbar)
                        val users = result.data
                        setUsers(users)
                    }
                }
            }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItemDone: MenuItem? = menu?.findItem(R.id.action_done)
        when (getSelectedUsers().size) {
            1 -> menuItemDone?.icon = resources.getDrawable(R.drawable.ic_account_check)
            else -> menuItemDone?.icon = resources.getDrawable(R.drawable.ic_account_multiple_check)
        }

        return super.onPrepareOptionsMenu(menu)
    }
}