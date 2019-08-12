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
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.data.User
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.viewmodels.UserListViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.ConnectycubeUsers
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber


class LoginActivity : BaseChatActivity() {
    private lateinit var users: ArrayList<ConnectycubeUser>
    private lateinit var adapter: ArrayAdapter<String>
    val isSignedIn: Boolean = false
    val isLoggedIn: Boolean
        get() = ConnectycubeChatService.getInstance().isLoggedIn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        actionBar?.setTitle(R.string.title_login_activity)
        SettingsProvider.initChat()
        makeLogin()
    }

    private fun makeLogin() {
        if (SharedPreferencesManager.getInstance(applicationContext).currentUserExists()) {
            showProgress(progressbar)
            val user = SharedPreferencesManager.getInstance(applicationContext).getCurrentUser()
            text_view.text = getString(R.string.user_logged_in, user.fullName ?: user.login)
            loginToChat(user)
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

                val userRaw: User? = listUser!!.find { it.login == user.login }
                Timber.d("proceed loginTo user= $userRaw" + ", conUser= " + userRaw!!.conUser)
                signInRestIdNeed(userRaw.conUser)
            }
        }
    }

    private fun isSignedInREST(user: ConnectycubeUser) =
        ConnectycubeSessionManager.getInstance().sessionParameters?.userId == user.id ?: false

    private fun signInRestIdNeed(user: ConnectycubeUser) {
        if (!isSignedInREST(user)) {
            signInRest(user)
        } else {
            loginToChat(user)
        }
    }

    private fun signInRest(user: ConnectycubeUser) {
        ConnectycubeUsers.signIn(user).performAsync(object : EntityCallback<ConnectycubeUser> {
            override fun onSuccess(conUser: ConnectycubeUser, args: Bundle) {
                SharedPreferencesManager.getInstance(applicationContext).saveCurrentUser(user)
                loginToChat(user)
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

    fun loginToChat(user: ConnectycubeUser) {
        Timber.d("loginToChat user= " + user)
        if (!isLoggedIn) {
            ConnectycubeChatService.getInstance().login(user, object : EntityCallback<Void> {
                override fun onSuccess(void: Void?, bundle: Bundle?) {
                    hideProgress(progressbar)
                    startDialogs()
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
        } else {
            hideProgress(progressbar)
            startDialogs()
        }
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
        users.forEachIndexed { index, _ -> userList.add(String.format(getString(R.string.user), index + 1)) }
        adapter = ArrayAdapter(this, R.layout.list_item_user_simple, userList)
        list_users.adapter = adapter
        list_users.choiceMode = ListView.CHOICE_MODE_SINGLE
        list_users.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            loginTo(users[position])
        }
    }
}
