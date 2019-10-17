package com.connectycube.messenger.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connectycube.messenger.data.UserRepository

class SelectFromExistUsersViewModelFactory(
    private val applicationContext: Application,
    private val usersRepository: UserRepository
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) =
        SelectFromExistUsersViewModel(applicationContext, usersRepository) as T
}