package com.connectycube.messenger

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.auth.session.ConnectycubeSessionManager
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.data.User
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.SAMPLE_CONFIG_FILE_NAME
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.utilities.getAllUsersFromFile
import com.connectycube.messenger.viewmodels.UserListViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.ConnectycubeUsers
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber


class LoginActivity : BaseActivity() {
    private lateinit var users: ArrayList<ConnectycubeUser>
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        actionBar?.setTitle(R.string.title_login_activity)
        makeLogin()
    }

    private fun makeLogin() {
        if (SharedPreferencesManager.getInstance(applicationContext).currentUserExists()) {
            showProgress(progressbar)
            val user = SharedPreferencesManager.getInstance(applicationContext).getCurrentUser()
            text_view.text = getString(R.string.user_logged_in, user.fullName ?: user.login)
            startNextScreen()
        } else {
            initUsers()
            initUserAdapter()
        }
    }

    private fun loginTo(user: ConnectycubeUser) {
        showProgress(progressbar)
        Timber.d("called loginTo user = $user")
        val usersLogins = ArrayList<String>()
        users.forEach { usersLogins.add(it.login) }

        val userListViewModel: UserListViewModel by viewModels {
            InjectorUtils.provideUserListViewModelFactory(this, usersLogins)
        }
        userListViewModel.getUsers().observe(this) { resource ->
            if (resource.status == Status.SUCCESS) {
                val listUser = resource.data

                val userRaw: User? = listUser?.find { it.login == user.login }
                userRaw?.let {
                    Timber.d("proceed loginTo user= $it, conUser= ${it.conUser}")
                    val userToLogin = it.conUser.also { it.password = user.password }
                    signInRestIdNeed(userToLogin)
                }
            }
        }
    }

    private fun isSignedInREST(user: ConnectycubeUser) =
        ConnectycubeSessionManager.getInstance().sessionParameters?.userId == user.id ?: false

    private fun signInRestIdNeed(user: ConnectycubeUser) {
        if (!isSignedInREST(user)) {
            signInRest(user)
        } else {
            startNextScreen()
        }
    }

    private fun signInRest(user: ConnectycubeUser) {
        ConnectycubeUsers.signIn(user).performAsync(object : EntityCallback<ConnectycubeUser> {
            override fun onSuccess(conUser: ConnectycubeUser, args: Bundle) {
                SharedPreferencesManager.getInstance(applicationContext).saveCurrentUser(user)
                startNextScreen()
            }

            override fun onError(ex: ResponseException) {
                hideProgress(progressbar)
                Toast.makeText(
                    applicationContext,
                    getString(R.string.login_chat_login_error, ex.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    fun startNextScreen() {
        hideProgress(progressbar)
        startDialogs()
    }

    fun startDialogs() {
        Timber.d("ChatDialogActivity.start")
        startChatDialogsActivity()
    }

    private fun startChatDialogsActivity() {
        val intent = Intent(this, ChatDialogActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initUsers() {
        users = getAllUsersFromFile(SAMPLE_CONFIG_FILE_NAME, this)
    }

    private fun initUserAdapter() {
        val userList: ArrayList<String> = ArrayList(users.size)
        users.forEach { userList.add(it.login) }
        adapter = ArrayAdapter(this, R.layout.list_item_user_simple, userList)
        list_users.adapter = adapter
        list_users.choiceMode = ListView.CHOICE_MODE_SINGLE
        list_users.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            loginTo(users[position])
        }
    }
}
