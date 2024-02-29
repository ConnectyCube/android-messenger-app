package com.connectycube.messenger

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.messenger.data.User
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.SAMPLE_CONFIG_FILE_NAME
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.utilities.getAllUsersFromFile
import com.connectycube.messenger.viewmodels.UserListViewModel
import com.connectycube.messenger.vo.Status
import kotlinx.android.synthetic.main.activity_login.*
import com.connectycube.ConnectyCube
import com.connectycube.core.ConnectycubeSessionManager
import com.connectycube.messenger.api.PushService
import com.connectycube.users.models.ConnectycubeUser
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
            signInRestIfNeed(user)
        } else {
            initUsers()
            initUserAdapter()
        }
    }

    private fun loginWithSession(user: ConnectycubeUser) {
        if (ConnectycubeSessionManager.isActiveSessionValid()) loginTo(user)
        showProgress(progressbar)
        ConnectyCube.createSession(successCallback = { session ->
            loginTo(user)
        }, errorCallback = { error ->
            hideProgress(progressbar)
            Timber.e(error, "loginWithSession")
        })
    }

    private fun loginTo(user: ConnectycubeUser) {
        showProgress(progressbar)
        Timber.d("called loginTo user = $user")
        val usersLogins = ArrayList<String>()
        users.forEach { it.login?.let { login -> usersLogins.add(login) } }

        val userListViewModel: UserListViewModel by viewModels {
            InjectorUtils.provideUserListViewModelFactory(this, usersLogins)
        }
        fun errorProcessing(msg: String) {
            hideProgress(progressbar)
            Toast.makeText(
                applicationContext,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
        userListViewModel.getUsers().observe(this) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    val listUser = resource.data

                    val userRaw: User? = listUser?.find { it.login == user.login }
                    if (userRaw != null) {
                        Timber.d("proceed loginTo user= $userRaw, conUser= ${userRaw.conUser}")
                        val userToLogin = userRaw.conUser.also { it.login = user.login; it.password = user.password }
                        signInRestIfNeed(userToLogin)
                    } else {
                        errorProcessing(getString(R.string.loading_users_empty))
                    }
                }
                Status.ERROR ->
                    errorProcessing(getString(R.string.loading_users_error, resource.message))
                Status.LOADING -> {
                }
            }
        }
    }

    private fun isSignedInREST(user: ConnectycubeUser) =
        ConnectycubeSessionManager.activeSession?.user?.id == user.id

    private fun signInRestIfNeed(user: ConnectycubeUser) {
        if (!isSignedInREST(user)) {
            signInRest(user)
        } else {
            startDialogsScreen()
        }
    }

    private fun signInRest(user: ConnectycubeUser) {
        ConnectyCube.createSession(user, { conUser ->
            SharedPreferencesManager.getInstance(applicationContext).saveCurrentUser(user)
            PushService.instance.subscribeToPushesIfNeed(this)
            startDialogsScreen()
        }, { error ->
            hideProgress(progressbar)
            Toast.makeText(
                applicationContext,
                getString(R.string.login_chat_login_error, error.message),
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    fun startDialogsScreen() {
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
        users.forEach { user -> user.login?.let { it -> userList.add(it) } }
        adapter = ArrayAdapter(this, R.layout.list_item_user_simple, userList)
        list_users.adapter = adapter
        list_users.choiceMode = ListView.CHOICE_MODE_SINGLE
        list_users.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            loginWithSession(users[position])
        }
    }
}
