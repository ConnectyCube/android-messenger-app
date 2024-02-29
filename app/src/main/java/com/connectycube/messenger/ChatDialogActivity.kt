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
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.ConnectyCube
import com.connectycube.messenger.adapters.ChatDialogAdapter
import com.connectycube.messenger.api.UserService
import com.connectycube.messenger.events.EVENT_CHAT_LOGIN
import com.connectycube.messenger.events.EventChatConnection
import com.connectycube.messenger.events.LiveDataBus
import com.connectycube.messenger.helpers.RTCSessionManager
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.messenger.utilities.setSingleOnClickListener
import com.connectycube.messenger.viewmodels.ChatDialogListViewModel
import com.connectycube.messenger.vo.Status
import kotlinx.android.synthetic.main.activity_chatdialogs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeMessage
import com.connectycube.chat.realtime.ConnectycubeMessageListener
import com.connectycube.chat.realtime.ConnectycubeMessageStatusListener
import com.connectycube.messenger.api.PushService
import com.connectycube.users.models.ConnectycubeUser
import timber.log.Timber

const val REQUEST_SETTING_SCREEN = 50
const val REQUEST_CHAT_DIALOG_ID = 55
const val EXTRA_DIALOG_ID = "chat_dialog_id"

class ChatDialogActivity : BaseChatActivity(), ChatDialogAdapter.ChatDialogAdapterCallback {

    private val chatDialogListViewModel: ChatDialogListViewModel by viewModels {
        InjectorUtils.provideChatDialogListViewModelFactory(this)
    }

    private lateinit var chatDialogAdapter: ChatDialogAdapter
    private val messageStatusListener: ConnectycubeMessageStatusListener =
        ChatMessagesStatusListener()
    private val allMessageListener: AllMessageListener = AllMessageListener()

    private var currentDialogId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setContentView(R.layout.activity_chatdialogs)
        initActionbar()
        initDialogsAdapter()
        initDialogsRecyclerView()
        subscribeUi()
    }

    override fun onResume() {
        super.onResume()
        setCurrentUser()
    }

    private fun initActionbar() {
        action_bar_view.setSingleOnClickListener { startSettingsActivity() }
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

        chatDialogAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                scrollUp()
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                scrollUp()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                scrollUp()
            }
        })
    }

    private fun scrollUp() {
        chats_recycler_view.scrollToPosition(0)
    }

    private fun subscribeUi() {
        Timber.d("subscribeUi")
        showProgress(progressbar)

        if (ConnectyCube.chat.isLoggedIn()) {
            initManagers()
        }

        LiveDataBus.subscribe(EVENT_CHAT_LOGIN, this, Observer<EventChatConnection> {
            if (it.error != null) {
                val errMsg =
                    if (it.error.message != null && !it.error.message!!.contains(getString(R.string.read_error))) {
                        it.error.message
                    } else getString(R.string.chat_connection_problem)
                Toast.makeText(
                    this,
                    getString(R.string.login_chat_error_format, errMsg),
                    Toast.LENGTH_LONG
                ).show()
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

    private fun updateDialogAdapter(listChats: List<ConnectycubeDialog>?) {
        chatDialogAdapter.submitList(listChats)
    }

    private fun initDialogsAdapter() {
        chatDialogAdapter = ChatDialogAdapter(this)
        chatDialogAdapter.callback = this
    }

    private fun initManagers() {
        ConnectyCube.chat.addMessageStatusListener(messageStatusListener)
        ConnectyCube.chat.addMessageListener(allMessageListener)
    }

    private fun unregisterChatManagers() {
        ConnectyCube.chat.removeMessageStatusListener(
            messageStatusListener
        )
        ConnectyCube.chat.removeMessageListener(allMessageListener)
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
            REQUEST_CHAT_DIALOG_ID -> {
                val chatDialogIdForUpdate = data.getStringExtra(EXTRA_DIALOG_ID)
                chatDialogIdForUpdate?.let { chatDialogListViewModel.updateChat(it) }
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
        startActivityForResult(intent, REQUEST_CHAT_DIALOG_ID)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onChatDialogSelected(chatDialog: ConnectycubeDialog) {
        currentDialogId = chatDialog.dialogId
        startChatActivity(chatDialog)
    }

    override fun onChatDialogsListUpdated(currentList: List<ConnectycubeDialog>) {
        chats_empty_layout.visibility = if (currentList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onChatDialogDelete(chatDialog: ConnectycubeDialog) {
        Timber.d("Try delete dialog= ${chatDialog.dialogId}")
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

    private fun startChatActivity(chat: ConnectycubeDialog) {
        val intent = Intent(this, ChatMessageActivity::class.java).apply {
            putExtra(EXTRA_CHAT, chat)
        }
        startActivityForResult(intent, REQUEST_CHAT_DIALOG_ID)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
        currentDialogId = null
        chatDialogListViewModel.chatLiveDataLazy.removeObservers(this)
        LiveDataBus.unregister(EVENT_CHAT_LOGIN)
        GlobalScope.launch(Dispatchers.Main) {
            PushService.instance.unsubscribe(applicationContext).await()
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

    private inner class AllMessageListener : ConnectycubeMessageListener {

        override fun onError(message: ConnectycubeMessage, ex: Throwable) {
            Timber.d("processError $ex")
        }

        override fun onMessage(message: ConnectycubeMessage) {
            Timber.d("processMessage chatMessage= ${message.body}, from senderId ${message.senderId}")
            if (message.senderId != (ConnectyCube.chat.userForLogin?.id ?: false)) {
                Timber.d("processMessage chatDialogListViewModel.updateChat chatMessage= ${message.body}")
                chatDialogListViewModel.updateChat(message.dialogId!!)
            }
        }
    }

    private inner class ChatMessagesStatusListener : ConnectycubeMessageStatusListener {
        override fun onMessageDelivered(messageId: String, dialogId: String, userId: Int) {
            Timber.d("processMessageDelivered messageId= $messageId")
            chatDialogListViewModel.updateMessageDeliveredStatus(messageId, userId)
        }

        override fun onMessageRead(messageId: String, dialogId: String, userId: Int) {
            Timber.d("processMessageRead messageID= $messageId")
            chatDialogListViewModel.updateMessageReadStatus(messageId, userId)
        }

    }
}