package com.connectycube.messenger

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.adapters.ChatDialogAdapter
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.ChatListViewModel
import com.connectycube.messenger.vo.Status
import timber.log.Timber

const val EXTRA_CHAT = "chat_dialog"
class ChatDialogsActivity : ComponentActivity(), ChatDialogAdapter.ChatDialogAdapterCallback {

    val chatViewModel: ChatListViewModel by viewModels {
        InjectorUtils.provideChatListViewModelFactory(this)
    }

    private lateinit var chatDialogAdapter: ChatDialogAdapter
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var emptyListView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setContentView(R.layout.activity_chatdialogs)
        initDialogsAdapter()
        initViews()
        initDialogsRecyclerView()
        subscribeUi()
    }

    private fun initViews() {
        emptyListView = findViewById(R.id.layout_chat_empty)
        chatsRecyclerView = findViewById(R.id.chatsRecyclerView)
    }

    private fun initDialogsRecyclerView() {
        chatsRecyclerView.layoutManager = LinearLayoutManager(this)
        chatsRecyclerView.itemAnimator = DefaultItemAnimator()
        chatsRecyclerView.adapter = chatDialogAdapter
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
    }

    override fun onChatDialogSelected(chatDialog: ConnectycubeChatDialog) {
        startChatActivity(chatDialog)
    }

    override fun onChatDialogsListUpdated(currentList: List<ConnectycubeChatDialog>) {
        emptyListView.visibility = if(currentList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun startChatActivity(chat : ConnectycubeChatDialog) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(EXTRA_CHAT, chat);
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}