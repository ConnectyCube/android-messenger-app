package com.connectycube.messenger.adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.core.helper.CollectionsUtil
import com.connectycube.messenger.R
import com.connectycube.messenger.utilities.loadChatDialogPhoto
import timber.log.Timber


typealias ClickListener = (ConnectycubeChatMessage) -> Unit

class ChatMessageAdapter(
    val context: Context,
    var chatDialog: ConnectycubeChatDialog,
    private val clickListener: ClickListener
) : PagedListAdapter<ConnectycubeChatMessage, ChatMessageAdapter.BaseChatMessageViewHolder>(diffCallback) {
    val TEXT_OUTCOMING = 1
    val TEXT_INCOMING = 2
    val ATTACH_IMAGE_OUTCOMING = 3
    val ATTACH_IMAGE_INCOMING = 4

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChatMessageViewHolder {
        Timber.d("onCreateViewHolder viewType= " + viewType)
        return when (viewType) {
            TEXT_OUTCOMING -> ChatMessageOutComingViewHolder(parent, R.layout.chat_outcoming_item)
            TEXT_INCOMING -> ChatMessageIncomingViewHolder(parent, R.layout.chat_incoming_item)
            ATTACH_IMAGE_OUTCOMING -> ChatAttachViewHolderChatMessageViewHolder(parent, R.layout.chat_incoming_item)
            ATTACH_IMAGE_INCOMING -> ChatAttachViewHolderChatMessageViewHolder(parent, R.layout.chat_incoming_item)
            else -> throw IllegalArgumentException("Wrong type of viewType= $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseChatMessageViewHolder, position: Int) {
        Timber.d("Binding view holder at position $position")
        val chatMessage = getItem(position)!!
        if (isIncoming(chatMessage) && !isRead(chatMessage)) {
            markAsReadMessage(chatMessage)
        }
        when (this.getItemViewType(position)) {
            TEXT_OUTCOMING, TEXT_INCOMING -> onBindTextViewHolder(holder as BaseChatMessageViewHolder, position)
            ATTACH_IMAGE_OUTCOMING, ATTACH_IMAGE_INCOMING -> onBindAttachViewHolder(holder as ChatAttachViewHolderChatMessageViewHolder, position)
        }

    }

    fun onBindTextViewHolder(holder: BaseChatMessageViewHolder, position: Int) {
        val message = getItem(position)
        with(holder) {
            bindTo(message!!)
            message.let {
                itemView.setOnClickListener {
                    clickListener(message)
                }
            }
        }
    }

    fun onBindAttachViewHolder(holder: ChatAttachViewHolderChatMessageViewHolder, position: Int) {
        val message = getItem(position)
        with(holder) {
            bindTo(message!!)
            message.let {
                itemView.setOnClickListener {
                    clickListener(message)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chatMessage = this.getItem(position)!!
        val isReceived = isIncoming(chatMessage)
        return if (withAttachment(chatMessage)) {
            if (isReceived) {
                ATTACH_IMAGE_INCOMING
            } else ATTACH_IMAGE_OUTCOMING
        } else if (isReceived) {
            TEXT_INCOMING
        } else TEXT_OUTCOMING
    }

    fun isIncoming(chatMessage: ConnectycubeChatMessage): Boolean {
        val localUser = ConnectycubeChatService.getInstance().user
        return chatMessage.senderId != localUser.id
    }

    fun withAttachment(chatMessage: ConnectycubeChatMessage): Boolean {
        val attachments = chatMessage.attachments
        return attachments != null && !attachments.isEmpty()
    }

    fun formatDate(seconds: Long): String {
        return DateUtils.formatDateTime(context, seconds * 1000L, DateUtils.FORMAT_ABBREV_TIME)
    }

    private fun markAsReadMessage(chatMessage: ConnectycubeChatMessage) {
        try {
            chatDialog.readMessage(chatMessage)
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    private fun isRead(chatMessage: ConnectycubeChatMessage): Boolean {
        val localUserId = ConnectycubeChatService.getInstance().user.id
        return !CollectionsUtil.isEmpty(chatMessage.readIds) && chatMessage.readIds.contains(localUserId)
    }

    companion object {
        /**
         * This diff callback informs the PagedListAdapter how to compute list differences when new
         * PagedLists arrive.
         */
        private val diffCallback = object : DiffUtil.ItemCallback<ConnectycubeChatMessage>() {
            override fun areItemsTheSame(oldItem: ConnectycubeChatMessage, newItem: ConnectycubeChatMessage): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ConnectycubeChatMessage,
                newItem: ConnectycubeChatMessage
            ): Boolean =
                oldItem == newItem
        }
    }

    open inner class BaseChatMessageViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(
        itemView
    ) {
        val dateView = itemView.findViewById<TextView>(R.id.text_message_date)
        /**
         * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
         * ViewHolder when Item is loaded.
         */
        open fun bindTo(message: ConnectycubeChatMessage) {
            dateView.text = formatDate(message.dateSent)
        }
    }

    open inner class BaseChatMessageTextViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) : BaseChatMessageViewHolder(
        LayoutInflater.from(parent.context).inflate(chatItem, parent, false)
    ) {
        private val bodyView = itemView.findViewById<TextView>(R.id.text_message_body)
        private var message: ConnectycubeChatMessage? = null

        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            this.message = message
            bodyView.text = message.body
        }
    }

    inner class ChatMessageIncomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) : BaseChatMessageTextViewHolder(parent, chatItem) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)

        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            loadChatDialogPhoto(chatDialog.type == ConnectycubeDialogType.PRIVATE,
                "",
                imgAvatar,
                context)
        }
    }

    inner class ChatMessageOutComingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) : BaseChatMessageTextViewHolder(parent, chatItem)

    inner class ChatAttachViewHolderChatMessageViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) : BaseChatMessageViewHolder(
        LayoutInflater.from(parent.context).inflate(chatItem, parent, false)
    ) {
        private val bodyView = itemView.findViewById<TextView>(R.id.text_message_body)
        private var message: ConnectycubeChatMessage? = null

        override fun bindTo(message: ConnectycubeChatMessage) {
            this.message = message
        }
    }
}