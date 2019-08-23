package com.connectycube.messenger

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
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
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.exception.ChatException
import com.connectycube.chat.listeners.ChatDialogMessageListener
import com.connectycube.chat.listeners.ChatDialogTypingListener
import com.connectycube.chat.listeners.MessageStatusListener
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.messenger.adapters.ChatMessageAdapter
import com.connectycube.messenger.adapters.ClickListener
import com.connectycube.messenger.api.ConnectycubeMessageSender
import com.connectycube.messenger.paging.Status
import com.connectycube.messenger.utilities.*
import com.connectycube.messenger.viewmodels.AttachmentViewModel
import com.connectycube.messenger.viewmodels.ChatMessageListViewModel
import com.connectycube.users.model.ConnectycubeUser
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_END
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import com.zhihu.matisse.listener.OnCheckedListener
import kotlinx.android.synthetic.main.activity_chatmessages.*
import kotlinx.android.synthetic.main.activity_chatmessages.avatar_img
import kotlinx.android.synthetic.main.activity_chatmessages.back_btn
import kotlinx.android.synthetic.main.activity_chatmessages.progressbar
import kotlinx.android.synthetic.main.activity_chatmessages.toolbar
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


const val REQUEST_CODE_CHOOSE = 23
const val TYPING_INTERVAL_MS: Long = 900

class ChatMessageActivity : BaseChatActivity() {

    private val clickListener: ClickListener = this::onMessageClicked
    private val messageListener: ChatDialogMessageListener = ChatMessageListener()
    private val messageStatusListener: MessageStatusListener = ChatMessagesStatusListener()
    private val messageTypingListener: ChatDialogTypingListener = ChatTypingListener()
    private val permissionsHelper = PermissionsHelper(this)
    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var chatDialog: ConnectycubeChatDialog
    private lateinit var modelChatMessageList: ChatMessageListViewModel
    private val occupants: HashMap<Int, ConnectycubeUser> = HashMap()
    private val membersNames: ArrayList<String> = ArrayList()

    val modelAttachment: AttachmentViewModel by viewModels {
        InjectorUtils.provideAttachmentViewModelFactory(this.application)
    }

    private lateinit var messageSender: ConnectycubeMessageSender

    private var clearTypingTimer: Timer? = null

    private val textTypingWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            chatDialog.sendStopTypingNotification()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            chatDialog.sendIsTypingNotification()
        }
    }

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
        modelChatMessageList.unreadCounter = chatDialog.unreadMessageCount
        initToolbar()
        initManagers()
        initChatAdapter()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        back_btn.setOnClickListener { onBackPressed() }
        toolbar_layout.setOnClickListener { startChatDetailsActivity() }
        loadChatDialogPhoto(this, chatDialog.isPrivate, chatDialog.photo, avatar_img)

        chat_message_name.text = chatDialog.name
        modelChatMessageList.getOccupants(chatDialog).observe(this, Observer { resource ->
            when (resource.status) {
                com.connectycube.messenger.vo.Status.LOADING -> {
                }
                com.connectycube.messenger.vo.Status.ERROR -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                com.connectycube.messenger.vo.Status.SUCCESS -> {
                    resource.data?.let {
                        occupants.putAll(resource.data.associateBy({ it.id }, { it }))
                    }
                    membersNames.run {
                        clear()
                        addAll(occupants.map { it.value.fullName ?: it.value.login })
                    }
                    if (!chatDialog.isPrivate) chat_message_members_typing.text = membersNames.joinToString()
                }
            }
        })
        input_chat_message.addTextChangedListener(textTypingWatcher)
    }

    private fun getChatMessageListViewModel(): ChatMessageListViewModel {
        val chatMessageListViewModel: ChatMessageListViewModel by viewModels {
            InjectorUtils.provideChatMessageListViewModelFactory(this.application, chatDialog)
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
        chatDialog.addIsTypingListener(messageTypingListener)

    }

    fun unregisterChatManagers() {
        ConnectycubeChatService.getInstance().messageStatusesManager.removeMessageStatusListener(messageStatusListener)
        chatDialog.removeMessageListrener(messageListener)
        chatDialog.removeIsTypingListener(messageTypingListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterChatManagers()
        input_chat_message.removeTextChangedListener(textTypingWatcher)
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
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_action_audio -> {
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    fun startChatDetailsActivity() {
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