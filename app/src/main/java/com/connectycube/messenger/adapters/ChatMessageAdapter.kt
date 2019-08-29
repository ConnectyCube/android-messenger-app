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
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.core.helper.CollectionsUtil
import com.connectycube.messenger.R
import com.connectycube.messenger.paging.NetworkState
import com.connectycube.messenger.utilities.loadAttachImage
import com.connectycube.messenger.utilities.loadChatMessagePhoto
import timber.log.Timber


typealias AttachmentClickListener = (ConnectycubeAttachment) -> Unit


class ChatMessageAdapter(
    val context: Context,
    var chatDialog: ConnectycubeChatDialog,
    private val attachmentClickListener: (ConnectycubeAttachment) -> Unit
) : PagedListAdapter<ConnectycubeChatMessage, RecyclerView.ViewHolder>(diffCallback) {
    val IN_PROGRESS = -1
    val TEXT_OUTCOMING = 1
    val TEXT_INCOMING = 2
    val ATTACH_IMAGE_OUTCOMING = 3
    val ATTACH_IMAGE_INCOMING = 4

    val localUserId = ConnectycubeChatService.getInstance().user.id
    val occupantIds = chatDialog.occupants.apply { remove(localUserId) }
    private var networkState: NetworkState? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Timber.d("onCreateViewHolder viewType= " + viewType)
        return when (viewType) {
            TEXT_OUTCOMING -> ChatMessageOutcomingViewHolder(parent, R.layout.chat_outcoming_item)
            TEXT_INCOMING -> ChatMessageIncomingViewHolder(parent, R.layout.chat_incoming_item)
            ATTACH_IMAGE_OUTCOMING -> ChatImageAttachOutcomingViewHolder(
                parent,
                R.layout.chat_outcoming_attachimage_item
            )
            ATTACH_IMAGE_INCOMING -> ChatImageAttachIncomingViewHolder(parent, R.layout.chat_incoming_attachimage_item)
            IN_PROGRESS -> NetworkStateItemViewHolder.create(parent)
            else -> throw IllegalArgumentException("Wrong type of viewType= $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        Timber.d("Binding view holder at position $position, payloads= ${payloads.isNotEmpty()}")
        if (payloads.isNotEmpty()) {
            val message = getItem(position)
            message?.let {
                val imgStatus = holder.itemView.findViewById<ImageView>(R.id.message_status_image_view)
                setStatus(imgStatus, message)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Timber.d("Binding view holder at position $position")
        val chatMessage = getItem(position)
        chatMessage?.let {
            if (isIncoming(chatMessage) && !isRead(chatMessage)) {
                markAsReadMessage(chatMessage)
            }
        }

        when (this.getItemViewType(position)) {
            TEXT_OUTCOMING, TEXT_INCOMING -> onBindTextViewHolder(holder as BaseChatMessageViewHolder, position)
            ATTACH_IMAGE_OUTCOMING, ATTACH_IMAGE_INCOMING -> onBindAttachViewHolder(
                holder as BaseChatMessageViewHolder,
                position
            )
            IN_PROGRESS -> (holder as NetworkStateItemViewHolder).bindTo(
                networkState
            )
        }
    }

    fun onBindTextViewHolder(holder: BaseChatMessageViewHolder, position: Int) {
        val message = getItem(position)
        with(holder) {
            bindTo(message!!)
        }
    }

    fun onBindAttachViewHolder(holder: BaseChatMessageViewHolder, position: Int) {
        val message = getItem(position)
        with(holder) {
            bindTo(message!!)
            message.let {
                itemView.setOnClickListener {
                    attachmentClickListener(message.attachments.first())
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chatMessage = this.getItem(position)
        chatMessage?.let {
            val isReceived = isIncoming(chatMessage)
            return if (withAttachment(chatMessage)) {
                if (isReceived) {
                    ATTACH_IMAGE_INCOMING
                } else ATTACH_IMAGE_OUTCOMING
            } else if (isReceived) {
                TEXT_INCOMING
            } else TEXT_OUTCOMING
        }
        return IN_PROGRESS
    }

    fun setStatus(imgStatus: ImageView?, msg: ConnectycubeChatMessage) {
        when {
            messageIsRead(msg) -> imgStatus?.setImageResource(R.drawable.ic_check_double_color_16)
            messageIsDelivered(msg) -> imgStatus?.setImageResource(R.drawable.ic_check_double_16)
            messageIsSent(msg) -> imgStatus?.setImageResource(R.drawable.ic_check_black_16dp)
            else -> imgStatus?.setImageResource(android.R.color.transparent)
        }
    }

    private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

    fun setNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newNetworkState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    fun isIncoming(chatMessage: ConnectycubeChatMessage): Boolean {
        val localUser = ConnectycubeChatService.getInstance().user
        return chatMessage.senderId != null && chatMessage.senderId != localUser.id
    }

    fun withAttachment(chatMessage: ConnectycubeChatMessage): Boolean {
        val attachments = chatMessage.attachments
        return attachments != null && !attachments.isEmpty()
    }

    fun formatDate(seconds: Long): String {
        return DateUtils.formatDateTime(context, seconds * 1000L, DateUtils.FORMAT_SHOW_TIME)
    }

    private fun markAsReadMessage(chatMessage: ConnectycubeChatMessage) {
        try {
            chatDialog.readMessage(chatMessage)
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    private fun isRead(chatMessage: ConnectycubeChatMessage): Boolean {
        return !CollectionsUtil.isEmpty(chatMessage.readIds) && chatMessage.readIds.contains(localUserId)
    }

    companion object {
        /**
         * This diff callback informs the PagedListAdapter how to compute list differences when new
         * PagedLists arrive.
         */
        private val PAYLOAD_STATUS = Any()
        private val diffCallback = object : DiffUtil.ItemCallback<ConnectycubeChatMessage>() {
            override fun areItemsTheSame(oldItem: ConnectycubeChatMessage, newItem: ConnectycubeChatMessage): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ConnectycubeChatMessage,
                newItem: ConnectycubeChatMessage
            ): Boolean =
                oldItem.id == newItem.id && oldItem.readIds == newItem.readIds && oldItem.deliveredIds == newItem.deliveredIds

            override fun getChangePayload(oldItem: ConnectycubeChatMessage, newItem: ConnectycubeChatMessage): Any? {
                return if (sameExceptStatus(oldItem, newItem)) {
                    PAYLOAD_STATUS
                } else null
            }

            fun sameExceptStatus(oldItem: ConnectycubeChatMessage, newItem: ConnectycubeChatMessage): Boolean {
                return newItem.readIds != oldItem.readIds || newItem.deliveredIds != oldItem.deliveredIds
            }
        }
    }

    private fun messageIsSent(message: ConnectycubeChatMessage): Boolean {
        return message.deliveredIds?.contains(localUserId)?: false
    }

    private fun messageIsRead(message: ConnectycubeChatMessage): Boolean {
        if (chatDialog.isPrivate) return message.readIds != null &&
                (message.recipientId == null || message.readIds.contains(message.recipientId))
        return message.readIds != null && message.readIds.any { it in occupantIds }
    }

    private fun messageIsDelivered(message: ConnectycubeChatMessage): Boolean {
        if (chatDialog.isPrivate) return message.deliveredIds?.contains(message.recipientId)?: false
        return message.deliveredIds != null && message.deliveredIds.any { it in occupantIds }
    }

    fun getAttachImageUrl(attachment: ConnectycubeAttachment): String {
        return attachment.url
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

    open inner class BaseChatMessageTextViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatMessageViewHolder(
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

    inner class ChatMessageIncomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatMessageTextViewHolder(parent, chatItem) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)

        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            loadChatMessagePhoto(
                chatDialog.type == ConnectycubeDialogType.PRIVATE,
                "",
                imgAvatar,
                context
            )
        }
    }

    inner class ChatMessageOutcomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatMessageTextViewHolder(parent, chatItem) {
        val imgStatus: ImageView = itemView.findViewById(R.id.message_status_image_view)
        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            setStatus(imgStatus, message)
        }
    }

    open inner class BaseChatImageAttachViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatMessageViewHolder(
            LayoutInflater.from(parent.context).inflate(chatItem, parent, false)
        ) {
        private val attachmentView: ImageView = itemView.findViewById(R.id.attachment_image_view)

        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            showImageAttachment(message)
        }

        private fun showImageAttachment(message: ConnectycubeChatMessage) {
            val validUrl = getAttachImageUrl(message.attachments.iterator().next())
            loadAttachImage(validUrl, attachmentView, context)
        }
    }

    inner class ChatImageAttachIncomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatImageAttachViewHolder(parent, chatItem) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)
        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            loadChatMessagePhoto(
                chatDialog.isPrivate,
                "",
                imgAvatar,
                context
            )
        }
    }

    inner class ChatImageAttachOutcomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatImageAttachViewHolder(parent, chatItem) {
        private val imgStatus: ImageView = itemView.findViewById(R.id.message_status_image_view)
        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            setStatus(imgStatus, message)
        }
    }
}