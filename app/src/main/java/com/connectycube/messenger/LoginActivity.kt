package com.connectycube.messenger

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.data.User
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.SAMPLE_CONFIG_FILE_NAME
import com.connectycube.messenger.utilities.SettingsProvider
import com.connectycube.messenger.utilities.getAllUsersFromFile
import com.connectycube.messenger.viewmodels.UserListViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.ConnectycubeUsers
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber


class LoginActivity : ComponentActivity() {
    private lateinit var users: ArrayList<ConnectycubeUser>
    private lateinit var adapter: ArrayAdapter<String>
    val isSignedIn:Boolean = false
    val isLoggedIn: Boolean
        get() = ConnectycubeChatService.getInstance().isLoggedIn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        actionBar?.setTitle(R.string.title_login_activity)
        SettingsProvider.initChat()
        initUsers()
        initUserAdapter()
    }

    fun loginTo(user: ConnectycubeUser) {
        showProgress()
        Timber.d("called loginTo user = $user")
        val usersLogins = ArrayList<String>()
        users.forEach { usersLogins.add(it.login) }

        val userViewModel: UserListViewModel by viewModels {
            InjectorUtils.provideUserListViewModelFactory(this, usersLogins)
        }
        userViewModel.getUsers().observe(this) { resource ->
            if (resource.status == Status.SUCCESS) {
                val listUser = resource.data

                val userRaw: User? = listUser!!.find { it.login == user.login }
                Timber.d("proceed loginTo user= $userRaw" + ", conUser= " + userRaw!!.conUser)
                signInRestIdNeed(userRaw.conUser)
            }
        }
    }

    fun signInRestIdNeed(user: ConnectycubeUser) {
        if(!isSignedIn) {
            signInRest(user)
        }
    }

    fun signInRest(user: ConnectycubeUser) {
        ConnectycubeUsers.signIn(user).performAsync(object : EntityCallback<ConnectycubeUser> {
            override fun onSuccess(User: ConnectycubeUser, args: Bundle) {
                loginToChat(user)
            }

            override fun onError(ex: ResponseException) {
                hideProgress()
                Toast.makeText(applicationContext, getString(R.string.login_chat_login_error, ex.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun loginToChat(user: ConnectycubeUser) {
        Timber.d("loginToChat user= " + user)
        if (!isLoggedIn) {
            ConnectycubeChatService.getInstance().login(user, object : EntityCallback<Void> {
                override fun onSuccess(void: Void?, bundle: Bundle?) {
                    hideProgress()
                    startDialogs()
                }

                override fun onError(ex: ResponseException) {
                    hideProgress()
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.login_chat_login_error, ex.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            hideProgress()
            startDialogs()
        }
    }

    fun startDialogs() {
        Timber.d("ChatDialogsActivity.start")
        startChatDialogsActivity()
    }

    private fun startChatDialogsActivity() {
        val intent = Intent(this, ChatDialogsActivity::class.java)
        startActivity(intent)
    }

    fun initUsers() {
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

    private fun hideProgress() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        progressbar.visibility = View.GONE;
    }

    private fun showProgress() {
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        );

        progressbar.visibility = View.VISIBLE;
    }

    override fun onBackPressed() {
        ConnectycubeChatService.getInstance().destroy()
        finish()
    }
}
