package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.exception.ChatException
import com.connectycube.chat.listeners.ChatDialogMessageListener
import com.connectycube.chat.listeners.MessageStatusListener
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.messenger.adapters.ChatMessageAdapter
import com.connectycube.messenger.adapters.ClickListener
import com.connectycube.messenger.api.ConnectycubeMessageSender
import com.connectycube.messenger.paging.Status
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.PermissionsHelper
import com.connectycube.messenger.utilities.REQUEST_ATTACHMENT_IMAGE_CONTACTS
import com.connectycube.messenger.viewmodels.ChatMessageListViewModel
import kotlinx.android.synthetic.main.activity_chatmessages.*
import timber.log.Timber
import com.zhihu.matisse.listener.OnCheckedListener
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.lifecycle.observe
import com.connectycube.messenger.utilities.Glide4Engine
import com.connectycube.messenger.viewmodels.AttachmentViewModel
import com.zhihu.matisse.internal.entity.CaptureStrategy
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType


const val REQUEST_CODE_CHOOSE = 23;

class ChatMessageActivity : BaseChatActivity() {

    private val clickListener: ClickListener = this::onMessageClicked
    private val messageListener: ChatDialogMessageListener = ChatMessageListener()
    private val messageStatusListener: MessageStatusListener = ChatMessagesStatusListener()
    private val permissionsHelper = PermissionsHelper(this)
    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var chatDialog: ConnectycubeChatDialog
    private lateinit var modelChatMessageList: ChatMessageListViewModel

    val modelAttachment: AttachmentViewModel by viewModels {
        InjectorUtils.provideAttachmentViewModelFactory(this.application)
    }

    private lateinit var messageSender: ConnectycubeMessageSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setContentView(R.layout.activity_chatmessages)
        chatDialog = intent.getSerializableExtra(EXTRA_CHAT) as ConnectycubeChatDialog
        chatDialog.initForChat(ConnectycubeChatService.getInstance())
        chatDialog.addMessageListener(messageListener)
        initChat()
        messageSender = ConnectycubeMessageSender(this, chatDialog)
        modelChatMessageList = getChatMessageListViewModel()
        initManagers()
        initChatAdapter()
    }

    private fun getChatMessageListViewModel(): ChatMessageListViewModel {
        val chatMessageListViewModel: ChatMessageListViewModel by viewModels {
            InjectorUtils.provideChatMessageListViewModelFactory(this, chatDialog)
        }
        return chatMessageListViewModel
    }

    private fun initChatAdapter() {
        chatAdapter = ChatMessageAdapter(this, chatDialog, clickListener)
        scroll_fb.setOnClickListener { scrollDown() }
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

        messages_recycleview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()

                val shouldShow = firstVisible >= 1
                if (totalItemCount > 0 && shouldShow) {
                    scroll_fb.show()
                    val shouldAdd =
                        scroll_fb.text.isEmpty() || scroll_fb.text.toString().toInt() != modelChatMessageList.unreadCounter
                    if (modelChatMessageList.unreadCounter > 0 && shouldAdd) {
                        scroll_fb.text = modelChatMessageList.unreadCounter.toString()
                        scroll_fb.extend()
                    }
                } else {
                    scroll_fb.shrink()
                    scroll_fb.hide()
                    scroll_fb.text = ""
                    modelChatMessageList.unreadCounter = 0
                }
            }
        })
        modelChatMessageList.refreshState.observe(this, Observer {
            Timber.d("refreshState= $it")
            if (it.status == Status.RUNNING && chatAdapter.itemCount == 0) {
                showProgress(progressbar)
            } else if (it.status == Status.SUCCESS) {
                hideProgress(progressbar)
            }
        })

        modelChatMessageList.networkState.observe(this, Observer {
            Timber.d("networkState= $it")
            chatAdapter.setNetworkState(it)
        })

        modelChatMessageList.messages.observe(this, Observer {
            Timber.d("submitList= ${it.size}")

            chatAdapter.submitList(it)
        })
    }

    private fun initChat() {
        when (chatDialog.type) {
            ConnectycubeDialogType.GROUP, ConnectycubeDialogType.BROADCAST -> {
                chatDialog.join(null)
            }

            ConnectycubeDialogType.PRIVATE -> Timber.d("ConnectycubeDialogType.PRIVATE type")

            else -> {
                Timber.d("Unsupported type")
                finish()
            }
        }
    }

    fun initManagers() {
        ConnectycubeChatService.getInstance().messageStatusesManager.addMessageStatusListener(messageStatusListener)
    }

    fun unregisterChatManagers() {
        ConnectycubeChatService.getInstance().messageStatusesManager.removeMessageStatusListener(messageStatusListener)
        chatDialog.removeMessageListrener(messageListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterChatManagers()
    }

    fun onAttachClick(view: View) {
        if (permissionsHelper.areAllImageGranted()) {
            Timber.d("onAttachClick areAllImageGranted")
            requestImageDevice()
        } else permissionsHelper.requestImagePermissions()
    }

    fun onSendChatClick(view: View) {
        val text = input_chat_message.text.toString().trim()
        if (text.isNotEmpty()) sendChatMessage(text)
    }

    private fun onMessageClicked(message: ConnectycubeChatMessage) {
        Timber.d("message= " + message)
    }

    fun requestImageDevice() {
        Matisse.from(this@ChatMessageActivity)
            .choose(MimeType.ofImage(), false)
            .countable(false)
            .capture(true)
            .captureStrategy(
                CaptureStrategy(true, "com.connectycube.messenger.fileprovider")
            )
            .maxSelectable(1)
//                .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
            .gridExpectedSize(
                resources.getDimensionPixelSize(R.dimen.grid_expected_size)
            )
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(Glide4Engine())
            .setOnSelectedListener { uriList, pathList ->
                // DO SOMETHING IMMEDIATELY HERE
                Timber.d("onSelected= pathList=$pathList")
            }
            .originalEnable(true)
            .maxOriginalSize(10)
//                .autoHideToolbarOnSingleTap(true)
            .setOnCheckedListener(OnCheckedListener { isChecked ->
                // DO SOMETHING IMMEDIATELY HERE
                Timber.d("isChecked= isChecked=$isChecked")
            })
            .forResult(REQUEST_CODE_CHOOSE)
    }

    fun sendChatMessage(text: String = "", attachment: ConnectycubeAttachment? = null) {
        messageSender.sendChatMessage(text, attachment).let {
            if (it.first) {
                if (ConnectycubeDialogType.PRIVATE == chatDialog.type) {
                    submitMessage(it.second)
                }
                input_chat_message.setText("")
            } else {
                Timber.d("sendChatMessage failed")
            }
        }

    }

    fun submitMessage(message: ConnectycubeChatMessage) {
        Timber.d("submitMessage modelChatMessageList.messages.value")
        modelChatMessageList.postItem(message)
    }

    fun scrollDown() {
        messages_recycleview.smoothScrollToPosition(0)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            if (data != null && Matisse.obtainPathResult(data) != null) {
                val path = Matisse.obtainPathResult(data).iterator().next()
                uploadAttachment(path, ConnectycubeAttachment.IMAGE_TYPE)
            }
        }
    }

    private fun uploadAttachment(path: String, type: String) {
        modelAttachment.uploadAttachment(path, type).observe(this) { resource ->
            when {
                resource.status == com.connectycube.messenger.vo.Status.LOADING -> {
                    showProgress(progressbar)
                    progressbar.progress = resource.progress ?: 0
                }

                resource.status == com.connectycube.messenger.vo.Status.SUCCESS -> {
                    hideProgress(progressbar)
                    Timber.d("resource.data=" + resource.data)
                    sendChatMessage(attachment = resource.data)
                }
                resource.status == com.connectycube.messenger.vo.Status.ERROR -> {
                    hideProgress(progressbar)
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.loading_attachment_error, resource.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_ATTACHMENT_IMAGE_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if (permissionsHelper.areAllImageGranted()) {
                    Timber.d("permission was granted")
                } else {
                    Timber.d("permission is denied")
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    internal inner class ChatMessageListener : ChatDialogMessageListener {
        override fun processMessage(dialogId: String, chatMessage: ConnectycubeChatMessage, senderId: Int) {
            Timber.d("ChatMessageListener processMessage " + chatMessage.body)
            submitMessage(chatMessage)
            if (senderId != ConnectycubeChatService.getInstance().user.id) {
                modelChatMessageList.unreadCounter++
            }
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

    inner class ChatMessagesStatusListener : MessageStatusListener {
        override fun processMessageRead(messageID: String, dialogId: String, userId: Int) {
            Timber.d("processMessageRead messageID= $messageID")
//            modelChatMessageList.refresh()
        }

        override fun processMessageDelivered(messageID: String, dialogId: String, userId: Int) {
            Timber.d("processMessageDelivered messageID= $messageID")
        }

    }
}