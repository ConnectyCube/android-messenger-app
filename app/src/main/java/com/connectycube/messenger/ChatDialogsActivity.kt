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
import com.connectycube.messenger.viewmodels.ChatListViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_chatdialogs.*
import timber.log.Timber

const val EXTRA_CHAT = "chat_dialog"
class ChatDialogsActivity : BaseChatActivity(), ChatDialogAdapter.ChatDialogAdapterCallback {

    private val chatViewModel: ChatListViewModel by viewModels {
        InjectorUtils.provideChatListViewModelFactory(this)
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
        currentDialogId?.let { chatViewModel.updateChat(currentDialogId!!)}
    }

    private fun initToolbar() {
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME
        title = getCurrentUser().fullName
    }

    private fun initDialogsRecyclerView() {
        chats_recycler_view.layoutManager = LinearLayoutManager(this)
        chats_recycler_view.itemAnimator = DefaultItemAnimator()
        chats_recycler_view.adapter = chatDialogAdapter
    }

    private fun subscribeUi() {
        Timber.d("subscribeUi")
        chatViewModel.getChats().observe(this) { resource ->
            if (resource.status == Status.SUCCESS) {
                val listChats = resource.data
                Timber.d("chatViewModel.getChats() = $listChats" + ", conUser= " + listChats!![0].conChat)
                updateDialogAdapter(listChats.map {chat -> chat.conChat})
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
        incomingMessagesManager?.dialogMessageListeners?.forEach {incomingMessagesManager?.removeDialogMessageListrener(it)}
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
        chats_empty_layout.visibility = if(currentList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun getCurrentUser(): ConnectycubeUser {
        return ConnectycubeChatService.getInstance().user
    }

    private fun startChatActivity(chat : ConnectycubeChatDialog) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(EXTRA_CHAT, chat)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private inner class AllMessageListener : ChatDialogMessageListener {
        override fun processError(p0: String?, p1: ChatException?, p2: ConnectycubeChatMessage?, p3: Int?) {
            Timber.d("processError")
        }

        override fun processMessage(dialogId: String, chatMessage: ConnectycubeChatMessage, senderId: Int?) {
            Timber.d("processMessage chatMessage= " + chatMessage.body + ", from senderId $senderId")
            if (senderId != ConnectycubeChatService.getInstance().user.id) {
                Timber.d("processMessage chatViewModel.updateChat chatMessage= " + chatMessage.body)
//                chatViewModel.updateChat(dialogId)
            }
        }
    }
}