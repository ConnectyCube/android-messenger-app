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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.messenger.adapters.AttachmentClickListener
import com.connectycube.messenger.adapters.ChatMessageAdapter
import com.connectycube.messenger.adapters.MarkAsReadListener
import com.connectycube.messenger.events.EVENT_CHAT_LOGIN
import com.connectycube.messenger.events.EventChatConnection
import com.connectycube.messenger.events.LiveDataBus
import com.connectycube.messenger.helpers.*
import com.connectycube.messenger.paging.Status
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.viewmodels.ChatMessageListViewModel
import com.connectycube.messenger.viewmodels.MessageSenderViewModel
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_END
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.zhihu.matisse.Matisse
import com.connectycube.ConnectyCube
import com.connectycube.chat.models.ConnectycubeAttachment
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeDialogType
import com.connectycube.chat.models.ConnectycubeMessage
import com.connectycube.chat.realtime.ConnectycubeChatTypingListener
import com.connectycube.chat.realtime.ConnectycubeMessageListener
import com.connectycube.chat.realtime.ConnectycubeMessageSentListener
import com.connectycube.messenger.databinding.ActivityChatmessagesBinding
import com.connectycube.users.models.ConnectycubeUser
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val TYPING_INTERVAL_MS: Long = 900
const val EXTRA_CHAT = "chat_dialog"
const val EXTRA_CHAT_ID = "chat_id"

const val REQUEST_CODE_DETAILS = 55

class ChatMessageActivity : BaseChatActivity() {

    private lateinit var binding: ActivityChatmessagesBinding
    private val attachmentClickListener: AttachmentClickListener = this::onMessageAttachmentClicked
    private val markAsReadListener: MarkAsReadListener = this::onMarkAsReadPerform
    private val messageListener: ChatMessageListener = ChatMessageListener()
    private val messageSentListener: ConnectycubeMessageSentListener = ChatMessagesSentListener()
    private val messageTypingListener: ConnectycubeChatTypingListener = ChatTypingListener()
    private val permissionsHelper = PermissionsHelper(this)
    private val layoutManager = LinearLayoutManager(this)
    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var chatDialog: ConnectycubeDialog
    private lateinit var modelChatMessageList: ChatMessageListViewModel
    private lateinit var modelMessageSender: MessageSenderViewModel
    private val occupants: HashMap<Int, ConnectycubeUser> = HashMap()
    private val membersNames: ArrayList<String> = ArrayList()
    private val localUserId = SharedPreferencesManager.getInstance(this).getCurrentUser().id

    private var clearTypingTimer: Timer? = null

    private val textTypingWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            ConnectyCube.chat.sendStopTypingStatus(chatDialog)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            ConnectyCube.chat.sendIsTypingStatus(chatDialog)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        binding = ActivityChatmessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        initWithData()
    }

    override fun onResume() {
        super.onResume()
        handleNotifications()
    }

    private fun initWithData() {
        if (intent.hasExtra(EXTRA_CHAT)) {
            val chatDialog = intent.getSerializableExtra(EXTRA_CHAT) as ConnectycubeDialog
            modelChatMessageList = getChatMessageListViewModel(chatDialog.dialogId!!)
        } else if (intent.hasExtra(EXTRA_CHAT_ID)) {
            val dialogId = intent.getStringExtra(EXTRA_CHAT_ID)
            modelChatMessageList = getChatMessageListViewModel(dialogId!!)
        }
        subscribeToDialog()
    }

    private fun handleNotifications() {
        if (intent.hasExtra(EXTRA_CHAT)) AppNotificationManager.getInstance().clearNotificationData(
            this, (intent.getSerializableExtra(EXTRA_CHAT) as ConnectycubeDialog).dialogId!!
        )
        if (intent.hasExtra(EXTRA_CHAT_ID)) AppNotificationManager.getInstance().clearNotificationData(
            this, intent.getStringExtra(EXTRA_CHAT_ID)!!
        )
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
        LiveDataBus.subscribe(EVENT_CHAT_LOGIN, this, Observer<EventChatConnection> {
            if (it.connected) {
                bindToChatConnection()
            } else {
                Toast.makeText(this, R.string.chat_connection_problem, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun bindChatDialog(chatDialog: ConnectycubeDialog) {
        this.chatDialog = chatDialog

        initChatAdapter()
        initToolbar()
        initModelSender()

        subscribeToOccupants()

        if (ConnectyCube.chat.isLoggedIn()) {
            bindToChatConnection()
        } else {
            subscribeToChatConnectionChanges()
        }
    }

    private fun initModelSender() {
        modelMessageSender = getMessageSenderViewModel()
    }

    private fun subscribeMessageSenderAttachment() {
        modelMessageSender.liveMessageAttachmentSender.observe(this, Observer { resource ->
            when {
                resource.status == com.connectycube.messenger.vo.Status.LOADING -> {
                    resource.progress?.let { progress ->
                        val msg = resource.data
                        val firstPosition = layoutManager.findFirstVisibleItemPosition()
                        val lastPosition = layoutManager.findLastVisibleItemPosition()

                        val range = firstPosition..lastPosition
                        range.forEach { position ->
                            if (msg?.messageId == chatAdapter.getItemByPosition(position)?.messageId) {
                                Timber.d("subscribeMessageSenderAttachment LOADING progress= $progress")
                                chatAdapter.updateAttachmentProgress(position, progress)
                            }
                        }
                    }
                }
                resource.status == com.connectycube.messenger.vo.Status.SUCCESS -> {
                    resource.data?.let {
                        submitMessage(it)
                    }
                }
                resource.status == com.connectycube.messenger.vo.Status.ERROR -> {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.sending_message_error, resource.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun subscribeMessageSenderText() {
        modelMessageSender.liveMessageSender.observe(this, Observer { resource ->
            when {
                resource.status == com.connectycube.messenger.vo.Status.LOADING -> {
                }
                resource.status == com.connectycube.messenger.vo.Status.SUCCESS -> {
                    resource.data?.let {
                        submitMessage(it)
                    }
                }
                resource.status == com.connectycube.messenger.vo.Status.ERROR -> {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.sending_message_error, resource.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun updateChatDialogData() {
        modelChatMessageList.getChatDialog().observe(this, Observer { resource ->
            when (resource.status) {
                com.connectycube.messenger.vo.Status.SUCCESS -> {
                    resource.data?.let { chatDialog ->
                        loadChatDialogPhoto(
                            this,
                            chatDialog.type == ConnectycubeDialogType.PRIVATE,
                            chatDialog.photo,
                            binding.avatarImg
                        )
                        binding.chatMessageName.text = chatDialog.name
                        subscribeToOccupants(chatDialog)
                    }
                }
                else -> {
                    // Ignore all other status.
                }
            }
        })
    }

    private fun bindToChatConnection() {
        subscribeMessageSenderAttachment()
        subscribeMessageSenderText()
        ConnectyCube.chat.addMessageListener(messageListener)

        initManagers()

        binding.inputChatMessage.addTextChangedListener(textTypingWatcher)
    }

    private fun initToolbar() {
        binding.backBtn.setOnClickListener { onBackPressed() }
        binding.toolbarLayout.setSingleOnClickListener { startChatDetailsActivity() }
        loadChatDialogPhoto(this, chatDialog.type == ConnectycubeDialogType.PRIVATE, chatDialog.photo, binding.avatarImg)
        binding.chatMessageName.text = chatDialog.name
    }

    private fun subscribeToOccupants(chatDialog: ConnectycubeDialog = this.chatDialog) {
        modelChatMessageList.getOccupants(chatDialog).observe(this, Observer { resource ->
            when (resource.status) {
                com.connectycube.messenger.vo.Status.LOADING -> {
                }
                com.connectycube.messenger.vo.Status.ERROR -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                com.connectycube.messenger.vo.Status.SUCCESS -> {
                    resource.data?.let {
                        val occupantsWithoutCurrent = resource.data.filter { it.id != localUserId }
                        occupants.putAll(occupantsWithoutCurrent.associateBy({ it.id }, { it }))
                        updateChatAdapter()
                    }

                    membersNames.run {
                        clear()
                        addAll(occupants.map { it.value.fullName ?: it.value.login!! })
                    }

                    if (chatDialog.type != ConnectycubeDialogType.PRIVATE) binding.chatMessageMembersTyping.text = membersNames.joinToString()
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

    private fun getMessageSenderViewModel(): MessageSenderViewModel {
        val messageSender: MessageSenderViewModel by viewModels {
            InjectorUtils.provideMessageSenderViewModelFactory(this.application, chatDialog)
        }
        return messageSender
    }

    private fun initChatAdapter() {
        chatAdapter = ChatMessageAdapter(this, chatDialog, attachmentClickListener, markAsReadListener)
        binding.scrollFb.setOnClickListener { scrollDown() }
        layoutManager.stackFromEnd = false
        layoutManager.reverseLayout = true
        binding.messagesRecycleview.layoutManager = layoutManager
        binding.messagesRecycleview.adapter = chatAdapter
        binding.messagesRecycleview.addItemDecoration(
            MarginItemDecoration(
                resources.getDimension(R.dimen.margin_normal).toInt()
            )
        )

        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0 && modelChatMessageList.scroll) {
                    scrollDown()
                }
            }
        })

        binding.messagesRecycleview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            fun shrinkFab() {
                binding.scrollFb.iconGravity = ICON_GRAVITY_START
                binding.scrollFb.shrink()
                binding.scrollFb.hide(false)
                binding.scrollFb.text = ""
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val totalItemCount = layoutManager.itemCount
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                var shouldShow = firstVisible >= 1
                if (dy < 0) {
//                    onScrolled Upwards
                } else if (dy > 0) {
//                    onScrolled Downwards
                    shouldShow = false
                }

                if (totalItemCount > 0 && shouldShow) {
                    if (!binding.scrollFb.isShown) {
                        binding.scrollFb.show(false)
                        binding.scrollFb.alpha = 0.3f
                    }
                    val count: String? =
                        Regex(pattern = "\\d+").find(input = binding.scrollFb.text.toString())?.value
                    val shouldAddCounter =
                        binding.scrollFb.text.isEmpty() || count?.toInt() != modelChatMessageList.unreadCounter
                    if (modelChatMessageList.unreadCounter > 0 && shouldAddCounter) {
                        binding.scrollFb.iconGravity = ICON_GRAVITY_TEXT_END
                        binding.scrollFb.text =
                            getString(
                                R.string.fbd_scroll_counter_label,
                                modelChatMessageList.unreadCounter.toString()
                            )
                        binding.scrollFb.extend()
                    }
                } else {
                    if (binding.scrollFb.isShown) shrinkFab()
                }
                if (firstVisible == 0) {
                    modelChatMessageList.unreadCounter = 0
                }
            }
        })
        modelChatMessageList.refreshState.observe(this, Observer {
            Timber.d("refreshState= $it")
            if (it.status == Status.RUNNING && chatAdapter.itemCount == 0) {
                showProgress(binding.progressbar)
            } else if (it.status == Status.SUCCESS) {
                hideProgress(binding.progressbar)
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

    private fun updateChatAdapter() {
        chatAdapter.setOccupants(occupants)
    }

    private fun initManagers() {
        ConnectyCube.chat.addTypingStatusListener(messageTypingListener)
        ConnectyCube.chat.addMessageSentListener(messageSentListener)
    }

    private fun unregisterChatManagers() {
        ConnectyCube.chat.removeMessageListener(messageListener)
        ConnectyCube.chat.removeTypingStatusListener(messageTypingListener)
        ConnectyCube.chat.removeMessageSentListener(messageSentListener)
    }

    private fun unsubscribeModels() {
        modelChatMessageList.messages.removeObservers(this)
        modelChatMessageList.networkState.removeObservers(this)
        modelChatMessageList.refreshState.removeObservers(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unsubscribeModels()
        if (ConnectyCube.chat.isLoggedIn()) {
            unregisterChatManagers()
            binding.inputChatMessage.removeTextChangedListener(textTypingWatcher)
        }
    }

    fun onAttachClick(view: View) {
        if (permissionsHelper.areAllImageGranted()) {
            Timber.d("onAttachClick areAllImageGranted")
            requestImage(this)
        } else permissionsHelper.requestImagePermissions()
    }

    fun onSendChatClick(view: View) {
        val text = binding.inputChatMessage.text.toString().trim()
        if (text.isNotEmpty()) sendChatMessage(text)
    }

    private fun onMessageAttachmentClicked(attachment: ConnectycubeAttachment, attachContainer: View) {
        Timber.d("message attachment= $attachment")
        startAttachmentPreview(attachment, attachContainer)
    }

    private fun onMarkAsReadPerform(chatMessage: ConnectycubeMessage) {
        ConnectyCube.chat.sendReadStatus(chatMessage, successCallback = {
            modelChatMessageList.updateItemReadStatus(chatMessage.messageId!!, localUserId)
        }, errorCallback = { ex ->
            Timber.d("readMessage ex= $ex")
        })
    }

    private fun startAttachmentPreview(attach: ConnectycubeAttachment, view: View) {
        startImagePreview(this, attach.url!!, getText(R.string.attachment_preview_label), view)
    }

    private fun sendChatMessage(text: String) {
        if (!ConnectyCube.chat.isLoggedIn()) {
            Toast.makeText(this, R.string.chat_connection_problem, Toast.LENGTH_LONG).show()
            return
        }
        modelChatMessageList.scroll = true
        modelMessageSender.sendMessage(text)
        binding.inputChatMessage.setText("")
    }

    fun submitMessage(message: ConnectycubeMessage) {
        Timber.d("submitMessage modelChatMessageList.messages.value")
        modelChatMessageList.postItem(message)
    }

    fun scrollDownIfNextToBottomList() {
        val firstPosition = layoutManager.findFirstVisibleItemPosition()
        val lastPosition = layoutManager.findLastVisibleItemPosition()

        val count = lastPosition - firstPosition

        if (firstPosition < count) {
            modelChatMessageList.scroll = true
        } else {
            modelChatMessageList.unreadCounter++
        }
    }

    fun scrollDown() {
        binding.messagesRecycleview.scrollToPosition(0)
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
        if (!ConnectyCube.chat.isLoggedIn()) {
            Toast.makeText(this, R.string.chat_connection_problem, Toast.LENGTH_LONG).show()
        } else {
            when (callType) {
                CALL_TYPE_VIDEO -> startVideoCall(
                    this,
                    ArrayList(chatDialog.occupantsIds!!.filter { it != ConnectyCube.chat.userForLogin!!.id })
                )
                CALL_TYPE_AUDIO -> startAudioCall(
                    this,
                    ArrayList(chatDialog.occupantsIds!!.filter { it != ConnectyCube.chat.userForLogin!!.id })
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            if (data != null && Matisse.obtainPathResult(data) != null) {
                val path = Matisse.obtainPathResult(data).iterator().next()
                uploadAttachment(path, "image", getString(R.string.message_attachment))
            }
        } else if(requestCode == REQUEST_CODE_DETAILS) {
            updateChatDialogData()
        }
    }

    private fun uploadAttachment(path: String, type: String, text: String) {
        modelChatMessageList.scroll = true
        modelMessageSender.sendAttachment(path, type, text)
    }

    private fun startChatDetailsActivity() {
        val intent = Intent(this, ChatDialogDetailsActivity::class.java)
        intent.putExtra(EXTRA_CHAT_DIALOG_ID, chatDialog.dialogId)
        startActivityForResult(intent, REQUEST_CODE_DETAILS)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun setDialogIdResult() {
        setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_DIALOG_ID, chatDialog.dialogId) })
    }

    override fun onBackPressed() {
        setDialogIdResult()
        super.onBackPressed()
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
                if (chatDialog.type != ConnectycubeDialogType.PRIVATE) binding.chatMessageMembersTyping.text = membersNames.joinToString()
                else binding.chatMessageMembersTyping.text = null
            }
        }
    }

    inner class ChatTypingListener : ConnectycubeChatTypingListener {

        override fun onUserIsTyping(dialogId: String?, userId: Int) {
            Timber.d("onUserIsTyping dialogId= $dialogId and chatDialog.dialogId= ${chatDialog.dialogId}")
            val isNotNullAndNotCurrentDialog = dialogId != null && chatDialog.dialogId != dialogId
            val isNotOpponentUserDialog = userId == ConnectyCube.chat.userForLogin!!.id
            val isNullAndNotPrivateDialog =
                dialogId == null && chatDialog.type != ConnectycubeDialogType.PRIVATE

            if (isNotOpponentUserDialog || isNullAndNotPrivateDialog || isNotNullAndNotCurrentDialog) return
            var userStatus = occupants[userId]?.fullName ?: occupants[userId]?.login
            userStatus?.let {
                userStatus = getString(R.string.chat_typing, userStatus)
            }
            binding.chatMessageMembersTyping.text = userStatus
            restartTypingTimer()
        }

        override fun onUserStopTyping(dialogId: String?, userId: Int) {
        }
    }

    inner class ChatMessageListener : ConnectycubeMessageListener {

        override fun onMessage(message: ConnectycubeMessage) {
            Timber.d("ChatMessageListener processMessage ${message.body}")
            val isIncoming = message.senderId != ConnectyCube.chat.userForLogin!!.id
            if (isIncoming) {
                scrollDownIfNextToBottomList()
                submitMessage(message)
            }
        }

        override fun onError(message: ConnectycubeMessage, ex: Throwable) {

        }
    }

    inner class MarginItemDecoration(private val spaceHeight: Int): StickyRecyclerHeadersDecoration(chatAdapter) {

        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView, state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            with(outRect) {
                if (parent.getChildAdapterPosition(view) == 0 && chatAdapter.isHeaderView(1)) {
                    top = spaceHeight * 4
                } else if (parent.getChildAdapterPosition(view) == 0) {
                    top = spaceHeight
                }
            }
        }
    }

    inner class ChatMessagesSentListener : ConnectycubeMessageSentListener {

        override fun onMessageSent(message: ConnectycubeMessage) {
            Timber.d("processMessageSent $message")
            modelChatMessageList.updateItemSentStatus(message.messageId!!, ConnectyCube.chat.userForLogin!!.id)
        }

        override fun onMessageSentFailed(message: ConnectycubeMessage) {
            Timber.d("processMessageFailed $message")
        }

    }
}