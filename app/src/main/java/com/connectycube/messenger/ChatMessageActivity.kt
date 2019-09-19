package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.auth.session.ConnectycubeSessionManager
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.exception.ChatException
import com.connectycube.chat.listeners.ChatDialogMessageListener
import com.connectycube.chat.listeners.ChatDialogMessageSentListener
import com.connectycube.chat.listeners.ChatDialogTypingListener
import com.connectycube.chat.listeners.MessageStatusListener
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.adapters.AttachmentClickListener
import com.connectycube.messenger.adapters.ChatMessageAdapter
import com.connectycube.messenger.api.ConnectycubeMessageSender
import com.connectycube.messenger.events.EVENT_CHAT_LOGIN
import com.connectycube.messenger.events.EventChatConnection
import com.connectycube.messenger.events.LiveDataBus
import com.connectycube.messenger.helpers.*
import com.connectycube.messenger.paging.Status
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.viewmodels.AttachmentViewModel
import com.connectycube.messenger.viewmodels.ChatMessageListViewModel
import com.connectycube.users.model.ConnectycubeUser
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_END
import com.zhihu.matisse.Matisse
import kotlinx.android.synthetic.main.activity_chatmessages.*
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val TYPING_INTERVAL_MS: Long = 900
const val EXTRA_CHAT = "chat_dialog"
const val EXTRA_CHAT_ID = "chat_id"

class ChatMessageActivity : BaseChatActivity() {

    private val attachmentClickListener: AttachmentClickListener = this::onMessageAttachmentClicked
    private val messageListener: ChatDialogMessageListener = ChatMessageListener()
    private val messageStatusListener: MessageStatusListener = ChatMessagesStatusListener()
    private val messageSentListener: ChatDialogMessageSentListener = ChatMessagesSentListener()
    private val messageTypingListener: ChatDialogTypingListener = ChatTypingListener()
    private val permissionsHelper = PermissionsHelper(this)
    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var chatDialog: ConnectycubeChatDialog
    private lateinit var modelChatMessageList: ChatMessageListViewModel
    private val occupants: HashMap<Int, ConnectycubeUser> = HashMap()
    private val membersNames: ArrayList<String> = ArrayList()

    private val modelAttachment: AttachmentViewModel by viewModels {
        InjectorUtils.provideAttachmentViewModelFactory(this.application)
    }

    private lateinit var messageSender: ConnectycubeMessageSender

    private var clearTypingTimer: Timer? = null

    private val textTypingWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            chatDialog.sendStopTypingNotification(object : EntityCallback<Void> {
                override fun onSuccess(v: Void?, b: Bundle?) {
                }

                override fun onError(ex: ResponseException?) {
                    Timber.e(ex)
                }
            })
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            chatDialog.sendIsTypingNotification(object : EntityCallback<Void> {
                override fun onSuccess(v: Void?, b: Bundle?) {
                }

                override fun onError(ex: ResponseException?) {
                    Timber.e(ex)
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setContentView(R.layout.activity_chatmessages)
        initWithData(intent)
    }

    private fun initWithData(intent: Intent) {
        if (intent.hasExtra(EXTRA_CHAT)){
            val chatDialog = intent.getSerializableExtra(EXTRA_CHAT) as ConnectycubeChatDialog
            modelChatMessageList = getChatMessageListViewModel(chatDialog.dialogId)
            bindChatDialog(chatDialog)
            AppNotificationManager.getInstance().clearNotificationData(this, chatDialog.dialogId)
        } else if (intent.hasExtra(EXTRA_CHAT_ID)){
            val dialogId = intent.getStringExtra(EXTRA_CHAT_ID)
            modelChatMessageList = getChatMessageListViewModel(dialogId)
            AppNotificationManager.getInstance().clearNotificationData(this, dialogId)
        }

        subscribeToDialog()
    }

    private fun subscribeToDialog() {
        modelChatMessageList.liveDialog.observe(this, Observer { resource ->
            when (resource.status) {
                com.connectycube.messenger.vo.Status.SUCCESS -> {
                    resource.data?.let { chatDialog ->
                        bindChatDialog(chatDialog)
                    }
                }
                com.connectycube.messenger.vo.Status.LOADING -> {
                }
                com.connectycube.messenger.vo.Status.ERROR -> {

                    resource.data?.let { chatDialog ->
                        bindChatDialog(chatDialog)
                    }
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun subscribeToChatConnectionChanges() {
        LiveDataBus.subscribe(EVENT_CHAT_LOGIN, this, Observer {
            val event = it as EventChatConnection

            if (event.connected) {
                bindToChatConnection()
            } else {
                Toast.makeText(this, R.string.chat_connection_problem, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun bindChatDialog(chatDialog: ConnectycubeChatDialog) {
        this.chatDialog = chatDialog

        modelChatMessageList.unreadCounter = chatDialog.unreadMessageCount ?: 0

        initChatAdapter()
        initToolbar()

        subscribeToOccupants()

        if (ConnectycubeChatService.getInstance().isLoggedIn) {
            bindToChatConnection()
        } else {
            subscribeToChatConnectionChanges()
        }
    }

    private fun bindToChatConnection() {
        chatDialog.initForChat(ConnectycubeChatService.getInstance())
        initChat(chatDialog)

        messageSender = ConnectycubeMessageSender(this, chatDialog)

        chatDialog.addMessageListener(messageListener)

        initManagers()

        input_chat_message.addTextChangedListener(textTypingWatcher)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        back_btn.setOnClickListener { onBackPressed() }
        toolbar_layout.setOnClickListener { startChatDetailsActivity() }
        loadChatDialogPhoto(this, chatDialog.isPrivate, chatDialog.photo, avatar_img)
        chat_message_name.text = chatDialog.name
    }

    private fun subscribeToOccupants() {
        modelChatMessageList.getOccupants(chatDialog).observe(this, Observer { resource ->
            when (resource.status) {
                com.connectycube.messenger.vo.Status.LOADING -> {
                }
                com.connectycube.messenger.vo.Status.ERROR -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                com.connectycube.messenger.vo.Status.SUCCESS -> {
                    resource.data?.let {
                        val occupantsWithoutCurrent = resource.data.filter { it.id != ConnectycubeSessionManager.getInstance().activeSession.userId }
                        occupants.putAll(occupantsWithoutCurrent.associateBy({ it.id }, { it }))
                    }

                    membersNames.run {
                        clear()
                        addAll(occupants.map { it.value.fullName ?: it.value.login })
                    }

                    if (!chatDialog.isPrivate) chat_message_members_typing.text = membersNames.joinToString()
                }
            }
        })
    }

    private fun getChatMessageListViewModel(dialogId: String): ChatMessageListViewModel {
        val chatMessageListViewModel: ChatMessageListViewModel by viewModels {
            InjectorUtils.provideChatMessageListViewModelFactory(this.application, dialogId)
        }
        return chatMessageListViewModel
    }

    private fun initChatAdapter() {
        chatAdapter = ChatMessageAdapter(this, chatDialog, attachmentClickListener)
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
                fun shrinkFab() {
                    scroll_fb.iconGravity = ICON_GRAVITY_START
                    scroll_fb.shrink()
                    scroll_fb.hide(false)
                    scroll_fb.text = ""
                    modelChatMessageList.unreadCounter = 0
                }

                if (modelChatMessageList.scroll) {
                    scrollDown()
                    shrinkFab()
                    return
                }

                val totalItemCount = layoutManager.itemCount
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                val shouldShow = firstVisible >= 1
                if (totalItemCount > 0 && shouldShow) {
                    if (!scroll_fb.isShown) {
                        scroll_fb.show(false)
                        scroll_fb.alpha = 0.3f
                    }
                    val count: String? = Regex(pattern = "\\d+").find(input = scroll_fb.text.toString())?.value
                    val shouldAddCounter =
                        scroll_fb.text.isEmpty() || count?.toInt() != modelChatMessageList.unreadCounter
                    if (modelChatMessageList.unreadCounter > 0 && shouldAddCounter) {
                        scroll_fb.iconGravity = ICON_GRAVITY_TEXT_END
                        scroll_fb.text =
                            getString(R.string.fbd_scroll_counter_label, modelChatMessageList.unreadCounter.toString())
                        scroll_fb.extend()
                    }
                } else {
                    if (scroll_fb.isShown) shrinkFab()
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

    private fun initChat(chatDialog: ConnectycubeChatDialog) {
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

    private fun initManagers() {
        ConnectycubeChatService.getInstance().messageStatusesManager.addMessageStatusListener(messageStatusListener)
        chatDialog.addIsTypingListener(messageTypingListener)
        chatDialog.addMessageSentListener(messageSentListener)
    }

    private fun unregisterChatManagers() {
        ConnectycubeChatService.getInstance().messageStatusesManager.removeMessageStatusListener(messageStatusListener)
        chatDialog.removeMessageListrener(messageListener)
        chatDialog.removeIsTypingListener(messageTypingListener)
        chatDialog.removeMessageSentListener(messageSentListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ConnectycubeChatService.getInstance().isLoggedIn) {
            unregisterChatManagers()
            input_chat_message.removeTextChangedListener(textTypingWatcher)
        }
    }

    fun onAttachClick(view: View) {
        if (permissionsHelper.areAllImageGranted()) {
            Timber.d("onAttachClick areAllImageGranted")
            requestImage(this)
        } else permissionsHelper.requestImagePermissions()
    }

    fun onSendChatClick(view: View) {
        val text = input_chat_message.text.toString().trim()
        if (text.isNotEmpty()) sendChatMessage(text)
    }

    private fun onMessageAttachmentClicked(attach: ConnectycubeAttachment) {
        Timber.d("message attachment= $attach")
        startAttachmentPreview(attach)
    }

    private fun startAttachmentPreview(attach: ConnectycubeAttachment) {
        val intent = Intent(this, AttachmentPreviewActivity::class.java)
        intent.putExtra(EXTRA_URL, attach.url)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun sendChatMessage(text: String = "", attachment: ConnectycubeAttachment? = null) {
        if (!ConnectycubeChatService.getInstance().isLoggedIn){
            Toast.makeText(this, R.string.chat_connection_problem, Toast.LENGTH_LONG).show()
            return
        }

        messageSender.sendChatMessage(text, attachment).let {
            if (it.first) {
                if (ConnectycubeDialogType.PRIVATE == chatDialog.type) {
                    submitMessage(it.second)
                    modelChatMessageList.scroll = true
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_message_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_video -> {
                startCall(CALL_TYPE_VIDEO)
                true
            }
            R.id.menu_action_audio -> {
                startCall(CALL_TYPE_AUDIO)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startCall(callType: Int) {
        if (!ConnectycubeChatService.getInstance().isLoggedIn){
            Toast.makeText(this, R.string.chat_connection_problem, Toast.LENGTH_LONG).show()
        } else {
            when (callType) {
                CALL_TYPE_VIDEO -> startVideoCall(
                    this,
                    ArrayList(chatDialog.occupants.filter { it != ConnectycubeChatService.getInstance().user.id })
                )
                CALL_TYPE_AUDIO -> startAudioCall(
                    this,
                    ArrayList(chatDialog.occupants.filter { it != ConnectycubeChatService.getInstance().user.id })
                )
            }
        }
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

    private fun startChatDetailsActivity() {
        val intent = Intent(this, ChatDialogDetailsActivity::class.java)
        intent.putExtra(EXTRA_CHAT_DIALOG_ID, chatDialog.dialogId)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_IMAGE -> {
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

    private fun restartTypingTimer() {
        clearTypingTimer?.cancel()
        startTimer()
    }

    private fun startTimer() {
        clearTypingTimer = Timer()
        clearTypingTimer?.schedule(TimerTypingTask(), TYPING_INTERVAL_MS)
    }

    inner class TimerTypingTask : TimerTask() {
        override fun run() {
            runOnUiThread {
                if (!chatDialog.isPrivate) chat_message_members_typing.text = membersNames.joinToString()
                else chat_message_members_typing.text = null
            }
        }
    }

    inner class ChatTypingListener : ChatDialogTypingListener {
        override fun processUserIsTyping(dialogId: String?, userId: Int?) {
            if (userId == ConnectycubeChatService.getInstance().user.id) return
            var userStatus = occupants[userId]?.fullName ?: occupants[userId]?.login
            userStatus?.let {
                userStatus = getString(R.string.chat_typing, userStatus)
            }
            chat_message_members_typing.text = userStatus
            restartTypingTimer()
        }

        override fun processUserStopTyping(dialogId: String?, userId: Int?) {

        }
    }

    inner class ChatMessageListener : ChatDialogMessageListener {
        override fun processMessage(dialogId: String, chatMessage: ConnectycubeChatMessage, senderId: Int) {
            Timber.d("ChatMessageListener processMessage " + chatMessage.body)
            val isIncoming = senderId != ConnectycubeChatService.getInstance().user.id
            if (isIncoming) {
                modelChatMessageList.unreadCounter++
            } else {
                modelChatMessageList.scroll = true
            }
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

    inner class ChatMessagesSentListener : ChatDialogMessageSentListener {
        override fun processMessageSent(dialogId: String, message: ConnectycubeChatMessage) {
             Timber.d("processMessageSent $message")
            modelChatMessageList.updateItemSentStatus(message.id, ConnectycubeChatService.getInstance().user.id)
        }

        override fun processMessageFailed(dialogId: String, message: ConnectycubeChatMessage) {
            Timber.d("processMessageFailed $message")
        }

    }

    inner class ChatMessagesStatusListener : MessageStatusListener {
        override fun processMessageRead(messageID: String, dialogId: String, userId: Int) {
            Timber.d("processMessageRead messageID= $messageID")
            modelChatMessageList.updateItemReadStatus(messageID, userId)
        }

        override fun processMessageDelivered(messageID: String, dialogId: String, userId: Int) {
            Timber.d("processMessageDelivered messageID= $messageID")
            modelChatMessageList.updateItemDeliveredStatus(messageID, userId)
        }

    }
}