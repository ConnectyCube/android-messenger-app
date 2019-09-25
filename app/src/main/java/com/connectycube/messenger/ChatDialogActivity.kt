package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.IncomingMessagesManager
import com.connectycube.chat.exception.ChatException
import com.connectycube.chat.listeners.ChatDialogMessageListener
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.adapters.ChatDialogAdapter
import com.connectycube.messenger.api.UserService
import com.connectycube.messenger.events.EVENT_CHAT_LOGIN
import com.connectycube.messenger.events.EventChatConnection
import com.connectycube.messenger.events.LiveDataBus
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.messenger.viewmodels.ChatDialogListViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_chatdialogs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

const val REQUEST_SETTING_SCREEN = 50

class ChatDialogActivity : BaseChatActivity(), ChatDialogAdapter.ChatDialogAdapterCallback {

    private val chatDialogListViewModel: ChatDialogListViewModel by viewModels {
        InjectorUtils.provideChatDialogListViewModelFactory(this)
    }

    private lateinit var chatDialogAdapter: ChatDialogAdapter
    private var incomingMessagesManager: IncomingMessagesManager? = null

    private var currentDialogId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setContentView(R.layout.activity_chatdialogs)
        initToolbar()
        initDialogsAdapter()
        initDialogsRecyclerView()
        subscribeUi()
    }

    override fun onResume() {
        super.onResume()
        setCurrentUser()
        currentDialogId?.let { chatDialogListViewModel.updateChat(currentDialogId!!) }
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setOnClickListener { startSettingsActivity() }
        setCurrentUser()
    }

    private fun setCurrentUser() {
        val currentUser = getCurrentUser()
        current_user_name.text = currentUser.fullName ?: currentUser.login
        loadUserAvatar(this, currentUser, avatar_img)
    }

    private fun initDialogsRecyclerView() {
        chats_recycler_view.layoutManager = LinearLayoutManager(this)
        chats_recycler_view.itemAnimator = DefaultItemAnimator()
        chats_recycler_view.adapter = chatDialogAdapter
    }

    private fun subscribeUi() {
        Timber.d("subscribeUi")
        showProgress(progressbar)
        LiveDataBus.subscribe(EVENT_CHAT_LOGIN, this, Observer<EventChatConnection> {
            if (it.error != null){
                Toast.makeText(this, getString(R.string.login_chat_error_format, it.error.message), Toast.LENGTH_LONG).show()
            } else if (it.connected) {
                initManagers()
            }
        })

        chatDialogListViewModel.chatLiveDataLazy.observe(this) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    hideProgress(progressbar)
                    val listChatDialogs = resource.data
                    Timber.d("chatDialogListViewModel.getChats() = ${listChatDialogs?.size}")
                    updateDialogAdapter(listChatDialogs)
                }
                Status.LOADING -> Timber.d("Status.LOADING")
                Status.ERROR -> hideProgress(progressbar)
            }
        }
    }

    private fun updateDialogAdapter(listChats: List<ConnectycubeChatDialog>?) {
        chatDialogAdapter.submitList(listChats)
    }

    private fun initDialogsAdapter() {
        chatDialogAdapter = ChatDialogAdapter(this)
        chatDialogAdapter.callback = this
    }

    fun initManagers() {
        incomingMessagesManager = ConnectycubeChatService.getInstance().incomingMessagesManager
        incomingMessagesManager?.addDialogMessageListener(AllMessageListener())
    }

    fun unregisterChatManagers() {
        incomingMessagesManager?.dialogMessageListeners?.forEach {
            incomingMessagesManager?.removeDialogMessageListrener(
                it
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED || data == null) return

        when (requestCode) {
            REQUEST_SETTING_SCREEN -> {
                if (data.getBooleanExtra(EXTRA_LOGOUT, false)) {
                    logout()
                }
            }
        }
    }

    fun startSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivityForResult(intent, REQUEST_SETTING_SCREEN)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterChatManagers()
    }

    fun onCreateNewChatClick(view: View) {
        val intent = Intent(this, CreateChatDialogActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onChatDialogSelected(chatDialog: ConnectycubeChatDialog) {
        currentDialogId = chatDialog.dialogId
        startChatActivity(chatDialog)
    }

    override fun onChatDialogsListUpdated(currentList: List<ConnectycubeChatDialog>) {
        chats_empty_layout.visibility = if (currentList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onChatDialogDelete(chatDialog: ConnectycubeChatDialog) {
        Timber.d("Try delete dialog " + chatDialog.dialogId)
        chatDialogListViewModel.deleteChat(chatDialog).observe(this) { resource ->
            when (resource.status) {
                Status.SUCCESS -> hideProgress(progressbar)
                Status.LOADING -> showProgress(progressbar)
                Status.ERROR -> hideProgress(progressbar)
            }
        }
    }

    private fun getCurrentUser(): ConnectycubeUser {
        return SharedPreferencesManager.getInstance(applicationContext).getCurrentUser()
    }

    private fun startChatActivity(chat: ConnectycubeChatDialog) {
        val intent = Intent(this, ChatMessageActivity::class.java).apply {
            putExtra(EXTRA_CHAT, chat)
        }
        startActivity(intent)
    }

    override fun onBackPressed() {
        //go home
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    private fun logout() {
        showProgress(progressbar)
        chatDialogListViewModel.chatLiveDataLazy.removeObservers(this)
        GlobalScope.launch(Dispatchers.Main) {
            UserService.instance.ultimateLogout(applicationContext)
            RTCSessionManager.getInstance().destroy()
            SharedPreferencesManager.getInstance(applicationContext).deleteCurrentUser()
            startLoginActivity()
            hideProgress(progressbar)

            finish()
        }
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this.startActivity(intent)
    }

    private inner class AllMessageListener : ChatDialogMessageListener {
        override fun processError(
            dialogId: String?,
            ex: ChatException?,
            msg: ConnectycubeChatMessage?,
            senderId: Int?
        ) {
            Timber.d("processError")
        }

        override fun processMessage(
            dialogId: String,
            chatMessage: ConnectycubeChatMessage,
            senderId: Int?
        ) {
            Timber.d("processMessage chatMessage= " + chatMessage.body + ", from senderId $senderId")
            if (senderId != ConnectycubeChatService.getInstance().user.id) {
                Timber.d("processMessage chatDialogListViewModel.updateChat chatMessage= " + chatMessage.body)
                chatDialogListViewModel.updateChat(dialogId)
            }
        }
    }
}