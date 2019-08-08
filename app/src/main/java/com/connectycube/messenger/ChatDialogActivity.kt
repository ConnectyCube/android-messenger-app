package com.connectycube.messenger

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.IncomingMessagesManager
import com.connectycube.chat.exception.ChatException
import com.connectycube.chat.listeners.ChatDialogMessageListener
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.adapters.ChatDialogAdapter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.ChatDialogListViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_chatdialogs.*
import timber.log.Timber

const val EXTRA_CHAT = "chat_dialog"

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
        initManagers()
        initToolbar()
        initDialogsAdapter()
        initDialogsRecyclerView()
        subscribeUi()
    }

    override fun onStart() {
        super.onStart()
        currentDialogId?.let { chatDialogListViewModel.updateChat(currentDialogId!!) }
    }

    private fun initToolbar() {
        supportActionBar?.displayOptions =
            ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME
        title = getCurrentUser().fullName
    }

    private fun initDialogsRecyclerView() {
        chats_recycler_view.layoutManager = LinearLayoutManager(this)
        chats_recycler_view.itemAnimator = DefaultItemAnimator()
        chats_recycler_view.adapter = chatDialogAdapter
    }

    private fun subscribeUi() {
        Timber.d("subscribeUi")
        chatDialogListViewModel.getChatDialogs().observe(this) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    hideProgress(progressbar)
                    val listChatDialogs = resource.data
                    Timber.d("chatDialogListViewModel.getChats() = $listChatDialogs" + ", conUser= " + listChatDialogs!![0])
                    updateDialogAdapter(listChatDialogs)
                }
                Status.LOADING -> showProgress(progressbar)
                Status.ERROR -> hideProgress(progressbar)
            }
        }
    }

    private fun updateDialogAdapter(listChats: List<ConnectycubeChatDialog>) {
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
        Toast.makeText(this, "Selected dialog " + chatDialog.dialogId, Toast.LENGTH_SHORT).show()
        currentDialogId = chatDialog.dialogId
        startChatActivity(chatDialog)
    }

    override fun onChatDialogsListUpdated(currentList: List<ConnectycubeChatDialog>) {
        chats_empty_layout.visibility = if (currentList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun getCurrentUser(): ConnectycubeUser {
        return ConnectycubeChatService.getInstance().user
    }

    private fun startChatActivity(chat: ConnectycubeChatDialog) {
        val intent = Intent(this, ChatMessageActivity::class.java)
        intent.putExtra(EXTRA_CHAT, chat)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        ConnectycubeChatService.getInstance().destroy()
        finish()
    }

    private inner class AllMessageListener : ChatDialogMessageListener {
        override fun processError(p0: String?, p1: ChatException?, p2: ConnectycubeChatMessage?, p3: Int?) {
            Timber.d("processError")
        }

        override fun processMessage(dialogId: String, chatMessage: ConnectycubeChatMessage, senderId: Int?) {
            Timber.d("processMessage chatMessage= " + chatMessage.body + ", from senderId $senderId")
            if (senderId != ConnectycubeChatService.getInstance().user.id) {
                Timber.d("processMessage chatDialogListViewModel.updateChat chatMessage= " + chatMessage.body)
                chatDialogListViewModel.updateChat(dialogId)
            }
        }
    }
}