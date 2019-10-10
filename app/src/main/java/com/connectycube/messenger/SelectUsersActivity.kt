package com.connectycube.messenger

import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.messenger.adapters.CheckableUsersAdapter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.SelectUsersViewModel
import com.connectycube.messenger.vo.Status
import kotlinx.android.synthetic.main.activity_create_chat.*

const val EXTRA_FILTER_IDS = "filter_ids"

class SelectUsersActivity : SelectUsersBaseActivity<SelectUsersViewModel>(),
    CheckableUsersAdapter.CheckableUsersAdapterCallback {

    override fun getViewMode(): SelectUsersViewModel {
        val selectedUsersViewModel: SelectUsersViewModel by viewModels {
            InjectorUtils.provideSelectUsersViewModelFactory(this.application)
        }
        return selectedUsersViewModel
    }

    override fun loadData() {
        super.loadData()

        getViewMode().getUsers(intent.extras.getIntegerArrayList(EXTRA_FILTER_IDS))
            .observe(this) { result ->
                when (result.status) {
                    Status.LOADING -> showProgress(progressbar)
                    Status.ERROR -> hideProgress(progressbar)
                    Status.SUCCESS -> {
                        hideProgress(progressbar)
                        val users = result.data
                        setUsers(users)
                    }
                }
            }
    }
}