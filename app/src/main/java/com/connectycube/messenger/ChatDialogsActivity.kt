package com.connectycube.messenger

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.connectycube.chat.ConnectycubeChatService
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setContentView(R.layout.activity_chatdialogs)
        initToolbar()
        initDialogsAdapter()
        initDialogsRecyclerView()
        subscribeUi()
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

    fun onCreateNewChatClick(view: View) {
        val intent = Intent(this, CreateChatDialogActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onChatDialogSelected(chatDialog: ConnectycubeChatDialog) {
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
}