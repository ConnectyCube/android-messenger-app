package com.connectycube.messenger

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.ChatListViewModel
import com.connectycube.messenger.vo.Status
import timber.log.Timber

const val EXTRA_CHAT = "chat_dialog"
class ChatDialogsActivity : ComponentActivity() {

    val chatViewModel: ChatListViewModel by viewModels {
        InjectorUtils.provideChatListViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setContentView(R.layout.activity_chatdialogs)
        initDialogsAdapter()
        subscribeUi()
    }

    private fun subscribeUi() {
        Timber.d("subscribeUi")
        chatViewModel.getChats().observe(this) { resource ->
            if (resource.status == Status.SUCCESS) {
                val listChats = resource.data
                Timber.d("chatViewModel.getChats() = $listChats" + ", conUser= " + listChats!![0].conChat)
                startChatActivity(listChats[1].conChat)
            }
        }
    }

    private fun initDialogsAdapter() {

    }

    fun onCreateNewChatClick(view: View) {

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