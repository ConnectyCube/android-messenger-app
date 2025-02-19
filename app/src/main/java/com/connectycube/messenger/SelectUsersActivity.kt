package com.connectycube.messenger

import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.SelectUsersViewModel
import com.connectycube.messenger.vo.Status

const val EXTRA_FILTER_IDS = "filter_ids"

class SelectUsersActivity : SelectUsersBaseActivity<SelectUsersViewModel>() {

    override fun getViewMode(): SelectUsersViewModel {
        val selectedUsersViewModel: SelectUsersViewModel by viewModels {
            InjectorUtils.provideSelectUsersViewModelFactory(this.application)
        }
        return selectedUsersViewModel
    }

    override fun loadData() {
        super.loadData()

        getViewMode().getUsers(intent.extras!!.getIntegerArrayList(EXTRA_FILTER_IDS)!!)
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
}