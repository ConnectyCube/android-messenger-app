package com.connectycube.messenger

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.exception.ChatException
import com.connectycube.chat.listeners.ChatDialogMessageListener
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.messenger.adapters.ChatMessageAdapter
import com.connectycube.messenger.adapters.ClickListener
import com.connectycube.messenger.api.ConnectycubeMessageSender
import com.connectycube.messenger.paging.NetworkState
import com.connectycube.messenger.paging.Status
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.viewmodels.ChatMessageViewModel
import kotlinx.android.synthetic.main.activity_chat.*
import timber.log.Timber

class ChatActivity : BaseChatActivity() {

    private val clickListener: ClickListener = this::onMessageClicked
    private val messageListener: ChatDialogMessageListener = ChatMessageListener()
    private val chatAdapter = ChatMessageAdapter(this, clickListener)
    private lateinit var chatDialog: ConnectycubeChatDialog
    private lateinit var model: ChatMessageViewModel
    private lateinit var messageSender: ConnectycubeMessageSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setContentView(R.layout.activity_chat)
        chatDialog = intent.getSerializableExtra(EXTRA_CHAT) as ConnectycubeChatDialog
        chatDialog.initForChat(ConnectycubeChatService.getInstance())
        chatDialog.addMessageListener(messageListener)

        messageSender = ConnectycubeMessageSender(this, chatDialog)
        val chatViewModel: ChatMessageViewModel by viewModels {
            InjectorUtils.provideMessageListViewModelFactory(this, chatDialog)
        }
        model = chatViewModel

        initChatAdapter()
    }

    private fun initChatAdapter() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = false
        layoutManager.reverseLayout = true
        messages_recycleview.layoutManager = layoutManager
        messages_recycleview.adapter = chatAdapter
        messages_recycleview.addItemDecoration(
            MarginItemDecoration(
                resources.getDimension(R.dimen.margin_normal).toInt()
            )
        )
        model.networkState.observe(this, Observer {
            Timber.d("networkState= $it")
            if(it.status.equals(Status.RUNNING)) {
                showProgress(progressbar)
            } else if(it.status == Status.SUCCESS) {
                hideProgress(progressbar)
            }

        })
        model.messages.observe(this, Observer<PagedList<ConnectycubeChatMessage>> {
            Timber.d("submitList= $it")
            chatAdapter.submitList(it)
            scrollDown()
        })
    }

    fun onAttachClick(view: View) {

    }

    fun onSendChatClick(view: View) {
        val text = input_chat_message.text.toString().trim { it <= ' ' }
        sendChatMessage(text)
    }

    private fun onMessageClicked(message: ConnectycubeChatMessage) {
        Timber.d("message= " + message)
    }

    fun sendChatMessage(text: String) {
        messageSender.sendChatMessage(text).let {
            if (it.first) {
                if (ConnectycubeDialogType.PRIVATE == chatDialog.type) {
                    submitMessage(it.second)
                    input_chat_message.setText("")
                }
            } else {
                Timber.d("sendChatMessage failed")
            }
        }

    }

    fun submitMessage(message: ConnectycubeChatMessage) {
        Timber.d("submitMessage model.messages.value?= " + model.messages.value)
        model.messages.value?.dataSource?.invalidate()
    }

    fun scrollDown() {
//        messages_recycleview.scrollToPosition(0)
        messages_recycleview.postDelayed({ messages_recycleview.scrollToPosition(0) }, 200)
    }

    internal inner class ChatMessageListener : ChatDialogMessageListener {
        override fun processMessage(s: String, chatMessage: ConnectycubeChatMessage, integer: Int?) {
            submitMessage(chatMessage)
        }
        override fun processError(s: String, e: ChatException, chatMessage: ConnectycubeChatMessage, integer: Int?) {

        }
    }

    class MarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView, state: RecyclerView.State
        ) {
            with(outRect) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    top = spaceHeight
                }
                left = spaceHeight
                right = spaceHeight
                bottom = spaceHeight
            }
        }
    }
}